from __future__ import annotations

import re
from collections import defaultdict

from sapiopycommons.general.exceptions import SapioException
from sapiopycommons.rules.on_save_rule_handler import OnSaveRuleHandler
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.DataRecord import DataRecord
from sapiopylib.rest.pojo.DataRecordPaging import DataRecordPojoPageCriteria
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopylib.rest.utils.recordmodel.PyRecordModel import PyRecordModel
from sapiopylib.rest.utils.recordmodel.properties import Children


class SprintOnSave(CommonsWebhookHandler):
    EMAIL_REGEX: re.Pattern[str] = re.compile(r"^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    TARGET_TYPES = ("Contact", "Developer", "Assignment")

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        try:
            save_handler: OnSaveRuleHandler = OnSaveRuleHandler(context)
        except SapioException:
            return SapioWebhookResult(
                True,
                display_text="Register this webhook on an on save rule.",
            )

        records_by_type: dict[str, list[PyRecordModel]] = {
            data_type: save_handler.get_models(data_type)
            for data_type in self.TARGET_TYPES
        }
        if not any(records_by_type.values()):
            return SapioWebhookResult(True)

        self.saved_vals_man.load(
            [
                record
                for records in records_by_type.values()
                for record in records
            ]
        )

        validation_errors: list[str] = []
        validation_errors.extend(self._validate_contact_emails(records_by_type["Contact"]))
        validation_errors.extend(
            self._validate_names(
                records_by_type["Contact"],
                records_by_type["Developer"],
            )
        )
        if validation_errors:
            self.callback.display_error("\n".join(validation_errors))
            return SapioWebhookResult(False)

        developer_records: dict[str, DataRecord] = self._load_developers_by_name()
        for assignment in records_by_type["Assignment"]:
            if not self._process_assignment_developer_change(assignment, developer_records):
                return SapioWebhookResult(False)

        self.rec_man.store_and_commit()
        return SapioWebhookResult(True)

    def _validate_contact_emails(self, contacts: list[PyRecordModel]) -> list[str]:
        errors: list[str] = []
        for contact in contacts:
            email: str = self._field_as_str(contact, "Email")
            previous_email: str = self._last_saved_as_str(contact, "Email")
            if email != previous_email and not self._is_valid_email(email):
                errors.append(f"{email} is not a valid email!")
        return errors

    def _validate_names(self, contacts: list[PyRecordModel], developers: list[PyRecordModel]) -> list[str]:
        for record in contacts + developers:
            name: str = self._field_as_str(record, "Name")
            previous_name: str = self._last_saved_as_str(record, "Name")
            if name != previous_name and "," in name:
                return ["Name entry cannot contain commas!"]
        return []

    def _process_assignment_developer_change(self, assignment: PyRecordModel, developer_records: dict[str, DataRecord]) -> bool:
        current_developer: str = self._field_as_str(assignment, "Developer")
        previous_developer: str = self._last_saved_as_str(assignment, "Developer")
        if current_developer == previous_developer:
            return True

        sprint_record: DataRecord | None = self._get_first_parent(assignment, "Sprint")
        if sprint_record is None:
            return True

        current_developer_record: DataRecord | None = (
            developer_records.get(current_developer) if current_developer else None
        )
        if current_developer and current_developer_record is None:
            return False

        sprint_assignments: list[DataRecord] = self._get_children(sprint_record, "Assignment")
        sprint_developers: list[DataRecord] = self._get_children(sprint_record, "Developer")
        developer_counts: dict[str, int] = self._count_assignment_developers(
            sprint_assignments,
            assignment,
            current_developer,
        )

        sprint_model: PyRecordModel = self.inst_man.add_existing_record(sprint_record)

        if previous_developer and developer_counts[previous_developer] == 0:
            previous_developer_record: DataRecord | None = developer_records.get(previous_developer)
            if previous_developer_record is not None:
                self._remove_child(sprint_model, previous_developer_record, sprint_developers)

        if current_developer_record is not None and developer_counts[current_developer] == 1:
            self._add_child(sprint_model, current_developer_record, sprint_developers)

        return True

    def _count_assignment_developers(self, sprint_assignments: list[DataRecord], changed_assignment: PyRecordModel, changed_developer: str) -> dict[str, int]:
        counts: dict[str, int] = defaultdict(int)
        for assignment_record in sprint_assignments:
            developer: str
            if assignment_record.record_id == changed_assignment.record_id:
                developer = changed_developer
            else:
                developer = self._record_field_as_str(assignment_record, "Developer")

            if developer:
                counts[developer] += 1
        return counts

    def _load_developers_by_name(self) -> dict[str, DataRecord]:
        developers_by_name: dict[str, DataRecord] = {}
        criteria: DataRecordPojoPageCriteria | None = DataRecordPojoPageCriteria(page_size=500)
        while criteria is not None:
            page = self.dr_man.query_all_records_of_type("Developer", criteria)
            for developer in page.result_list or []:
                name: str = self._record_field_as_str(developer, "Name")
                if name:
                    developers_by_name[name] = developer
            criteria = page.next_page_criteria if page.is_next_page_available else None
        return developers_by_name

    def _get_first_parent(self, record: PyRecordModel, parent_type: str) -> DataRecord | None:
        result = self.dr_man.get_parents_list(
            [record.record_id],
            record.data_type_name,
            parent_type,
        )
        parent_lists = result.result_map or {}
        parents: list[DataRecord] = parent_lists.get(record.record_id) or parent_lists.get(str(record.record_id)) or []
        return parents[0] if parents else None

    def _get_children(self, parent_record: DataRecord, child_type: str) -> list[DataRecord]:
        result = self.dr_man.get_children_list([parent_record.record_id], child_type)
        child_lists = result.result_map or {}
        return child_lists.get(parent_record.record_id) or child_lists.get(str(parent_record.record_id)) or []

    def _add_child(self, sprint_model: PyRecordModel, developer_record: DataRecord, sprint_developers: list[DataRecord]) -> None:
        if self._contains_record(sprint_developers, developer_record):
            return
        developer_model: PyRecordModel = self.inst_man.add_existing_record(developer_record)
        sprint_model.add(Children.refs([developer_model]))

    def _remove_child(self, sprint_model: PyRecordModel, developer_record: DataRecord, sprint_developers: list[DataRecord]) -> None:
        if not self._contains_record(sprint_developers, developer_record):
            return

        developer_model: PyRecordModel = self.inst_man.add_existing_record(developer_record)
        sprint_model.remove(Children.refs([developer_model]))

    @staticmethod
    def _contains_record(records: list[DataRecord], target: DataRecord) -> bool:
        return any(record.record_id == target.record_id for record in records)

    def _field_as_str(self, record: PyRecordModel, field_name: str) -> str:
        return self._value_as_str(record.get_field_value(field_name))

    def _last_saved_as_str(self, record: PyRecordModel, field_name: str) -> str:
        return self._value_as_str(self.saved_vals_man.get_last_saved_value(record, field_name))

    @staticmethod
    def _record_field_as_str(record: DataRecord, field_name: str) -> str:
        return SprintOnSave._value_as_str(record.get_fields().get(field_name))

    @staticmethod
    def _value_as_str(value: object | None) -> str:
        return "" if value is None else str(value)

    @classmethod
    def _is_valid_email(cls, email: str) -> bool:
        return bool(email and cls.EMAIL_REGEX.fullmatch(email))
