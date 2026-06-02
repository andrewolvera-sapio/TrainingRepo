from __future__ import annotations

from sapiopycommons.callbacks.callback_util import BlankResultHandling
from sapiopycommons.general.exceptions import SapioUserCancelledException
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.DataRecord import DataRecord
from sapiopylib.rest.pojo.DataRecordPaging import DataRecordPojoPageCriteria
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopylib.rest.utils.recordmodel.PyRecordModel import PyRecordModel
from sapiopylib.rest.utils.recordmodel.properties import Children


class ChangeCaptainButton(CommonsWebhookHandler):
    SPRINT_TYPE: str = "Sprint"
    DEVELOPER_TYPE: str = "Developer"
    ASSIGNMENT_TYPE: str = "Assignment"
    CHANGE_CAPTAIN_FIELD: str = "ChangeCaptain"
    CAPTAIN_FIELD: str = "Captain"
    DEVELOPER_FIELD: str = "Developer"
    NAME_FIELD: str = "Name"
    ACTIVE_FIELD: str = "Active"
    ROLE_FIELD: str = "Role"
    LEADER_ROLE: str = "Leader"

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        if not self._should_run(context):
            return SapioWebhookResult(True)

        sprint_record: DataRecord | None = context.data_record
        if sprint_record is None:
            return SapioWebhookResult(True)

        developers_by_name: dict[str, DataRecord] = self._load_developers_by_name()
        active_leaders: list[str] = sorted(
            name
            for name, developer in developers_by_name.items()
            if self._is_active_leader(developer)
        )

        try:
            selection: list[str] = self.callback.list_dialog(
                "Choose Developer",
                active_leaders,
                multi_select=False,
                blank_result_handling=BlankResultHandling.DEFAULT,
            )
        except SapioUserCancelledException:
            return SapioWebhookResult(True)

        if not selection:
            return SapioWebhookResult(True)

        selected_name: str = selection[0]
        selected_developer: DataRecord | None = developers_by_name.get(selected_name)
        if selected_developer is None:
            return SapioWebhookResult(True)

        sprint_model: PyRecordModel = self.inst_man.add_existing_record(sprint_record)
        sprint_developers: list[DataRecord] = self._get_children(
            sprint_record,
            self.DEVELOPER_TYPE,
        )

        self._remove_previous_captain_if_unassigned(
            sprint_record,
            sprint_model,
            sprint_developers,
        )

        if not self._contains_developer_name(sprint_developers, selected_name):
            selected_model: PyRecordModel = self.inst_man.add_existing_record(selected_developer)
            sprint_model.add(Children.refs([selected_model]))

        sprint_model.set_field_value(self.CAPTAIN_FIELD, selected_name)
        self.rec_man.store_and_commit()
        return SapioWebhookResult(True)

    def _should_run(self, context: SapioWebhookContext) -> bool:
        data_type_name: str = self._context_value_as_str(
            context,
            "data_type_name",
            "dataTypeName",
        )
        data_field_name: str = self._context_value_as_str(
            context,
            "data_field_name",
            "dataFieldName",
        )
        return (
            data_type_name == self.SPRINT_TYPE
            and data_field_name == self.CHANGE_CAPTAIN_FIELD
        )

    def _load_developers_by_name(self) -> dict[str, DataRecord]:
        developers_by_name: dict[str, DataRecord] = {}
        criteria: DataRecordPojoPageCriteria | None = DataRecordPojoPageCriteria(page_size=500)
        while criteria is not None:
            page = self.dr_man.query_all_records_of_type(self.DEVELOPER_TYPE, criteria)
            for developer in page.result_list or []:
                name: str = self._record_field_as_str(developer, self.NAME_FIELD)
                if name:
                    developers_by_name[name] = developer
            criteria = page.next_page_criteria if page.is_next_page_available else None
        return developers_by_name

    def _is_active_leader(self, developer: DataRecord) -> bool:
        fields: dict[str, object] = developer.get_fields()
        return (
            fields.get(self.ACTIVE_FIELD) is not False
            and fields.get(self.ROLE_FIELD) == self.LEADER_ROLE
        )

    def _remove_previous_captain_if_unassigned(self, sprint_record: DataRecord, sprint_model: PyRecordModel, sprint_developers: list[DataRecord]) -> None:
        old_captain: str = self._record_field_as_str(sprint_record, self.CAPTAIN_FIELD)
        if not old_captain:
            return

        for developer in sprint_developers:
            if old_captain != self._record_field_as_str(developer, self.NAME_FIELD):
                continue

            if not self._is_child_of_assignments(sprint_record, old_captain):
                developer_model: PyRecordModel = self.inst_man.add_existing_record(developer)
                sprint_model.remove(Children.refs([developer_model]))
            return

    def _is_child_of_assignments(self, sprint_record: DataRecord, developer_name: str) -> bool:
        assignments: list[DataRecord] = self._get_children(
            sprint_record,
            self.ASSIGNMENT_TYPE,
        )
        return any(
            developer_name == self._record_field_as_str(assignment, self.DEVELOPER_FIELD)
            for assignment in assignments
        )

    def _get_children(self, parent_record: DataRecord, child_type: str) -> list[DataRecord]:
        result = self.dr_man.get_children_list([parent_record.record_id], child_type)
        child_lists = result.result_map or {}
        return (
            child_lists.get(parent_record.record_id)
            or child_lists.get(str(parent_record.record_id))
            or []
        )

    def _contains_developer_name(self, developers: list[DataRecord], developer_name: str) -> bool:
        return any(
            developer_name == self._record_field_as_str(developer, self.NAME_FIELD)
            for developer in developers
        )

    @staticmethod
    def _context_value_as_str(context: SapioWebhookContext, *names: str) -> str:
        for name in names:
            value: object | None = getattr(context, name, None)
            if value is not None:
                return str(value)
        return ""

    @staticmethod
    def _record_field_as_str(record: DataRecord, field_name: str) -> str:
        value: object | None = record.get_fields().get(field_name)
        return "" if value is None else str(value)
