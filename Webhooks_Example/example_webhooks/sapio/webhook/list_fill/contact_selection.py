from __future__ import annotations

from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.DataRecord import DataRecord
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult


class SprintContactSelection(CommonsWebhookHandler):
    SPRINT_TYPE: str = "Sprint"
    ACCOUNT_TYPE: str = "Account"
    CONTACT_TYPE: str = "Contact"
    CONTACT_FIELD: str = "Contact"
    NAME_FIELD: str = "Name"

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        if not self._is_sprint_contact_selection(context):
            return SapioWebhookResult(True, list_values=[])

        sprint_record: DataRecord | None = context.data_record
        if sprint_record is None:
            return SapioWebhookResult(True, list_values=[])

        account_record: DataRecord | None = self._get_first_parent(
            sprint_record,
            self.SPRINT_TYPE,
            self.ACCOUNT_TYPE,
        )
        if account_record is None:
            return SapioWebhookResult(True, list_values=[])

        contact_records: list[DataRecord] = self._get_children(
            account_record,
            self.CONTACT_TYPE,
        )
        contact_names: list[str] = [
            name
            for name in (
                self._record_field_as_str(contact, self.NAME_FIELD)
                for contact in contact_records
            )
            if name
        ]

        return SapioWebhookResult(True, list_values=contact_names)

    def _is_sprint_contact_selection(self, context: SapioWebhookContext) -> bool:
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
        return data_type_name == self.SPRINT_TYPE and data_field_name == self.CONTACT_FIELD

    def _get_first_parent(
        self,
        record: DataRecord,
        data_type_name: str,
        parent_type: str,
    ) -> DataRecord | None:
        result = self.dr_man.get_parents_list(
            [record.record_id],
            data_type_name,
            parent_type,
        )
        parent_lists = result.result_map or {}
        parents: list[DataRecord] = (
            parent_lists.get(record.record_id)
            or parent_lists.get(str(record.record_id))
            or []
        )
        return parents[0] if parents else None

    def _get_children(self, parent_record: DataRecord, child_type: str) -> list[DataRecord]:
        result = self.dr_man.get_children_list([parent_record.record_id], child_type)
        child_lists = result.result_map or {}
        return (
            child_lists.get(parent_record.record_id)
            or child_lists.get(str(parent_record.record_id))
            or []
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
