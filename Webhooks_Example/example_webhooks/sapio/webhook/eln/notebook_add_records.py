from __future__ import annotations
from ast import List

from sapiopycommons.callbacks.callback_util import BlankResultHandling
from sapiopycommons.callbacks.field_builder import AnyFieldInfo, FieldBuilder
from sapiopycommons.general.custom_report_util import CustomReportUtil
from sapiopycommons.general.exceptions import SapioUserCancelledException
from sapiopylib.rest.pojo.CustomReport import CustomReportCriteria, RawReportTerm, RawTermOperation, ReportColumn
from sapiopylib.rest.pojo.datatype.FieldDefinition import FieldType
from sapiopycommons.eln.experiment_step_factory import ExperimentStepFactory
from sapiopycommons.general.aliases import SapioWebhookContext
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.WebhookService import SapioWebhookResult
from sapiopylib.rest.pojo.datatype.FieldDefinition import VeloxIntegerFieldDefinition
from sapiopylib.rest.pojo.DataRecord import DataRecord
from sapiopylib.rest.ELNService import ElnExperiment
from sapiopylib.rest.pojo.eln.ExperimentEntry import ExperimentTableEntry

class NotebookAddRecords(CommonsWebhookHandler):

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        data_type_names: List[str] = self.dt_man.get_data_type_name_list()

        dtSelection: List[str] = self.callback.list_dialog("Data Types", data_type_names, False, blank_result_handling=BlankResultHandling.CANCEL)
        dtName: str = dtSelection[0]
        options: List[str] = ["New", "Existing", "Cancel"]
        new_existing_choice: str = self.callback.option_dialog(
            "New or Existing",
            "Choose to create a new record, or select an existing",
            options,
            0,
            True,
        )

        if new_existing_choice == "New":
            amount: str = self.callback.input_dialog(f"Create Records", f"How many {dtName}s to create?", VeloxIntegerFieldDefinition(
                    "TempWebhookInput",
                    "RecordCount",
                    "Number of Records",
                    min_value=1,
                    max_value=100,
                    editable=True
                ),
                blank_result_handling=BlankResultHandling.REPEAT)
            numCreate = int(amount)
            created_records: List[DataRecord] = self.dr_man.add_data_records(dtName, numCreate)
            self.dr_man.commit_data_records(created_records)
            self._create_table_entry_with_records(dtName, created_records)

        elif new_existing_choice == "Existing":
            record_list: List[DataRecord] = self._select_existing_records(dtName)
            if record_list:
                self._create_table_entry_with_records(dtName, record_list)

        return SapioWebhookResult(True)

    def _select_existing_records(self, data_type_name: str) -> List[DataRecord]:
        dt_def = self.dt_man.get_data_type_definition(data_type_name)
        if dt_def is None:
            return []

        columns: List[ReportColumn] = [ReportColumn(data_type_name, "RecordId", FieldType.LONG)]
        display_field_names: List[str] = ["RecordId"]
        field_list: List = self.dt_man.get_field_definition_list(data_type_name) or []
        for field_def in sorted(field_list, key=lambda f: f.data_field_name):
            field_name = field_def.data_field_name
            if field_def.system_field or field_name == "RecordId":
                continue
            columns.append(ReportColumn(data_type_name, field_name, field_def.data_field_type))
            display_field_names.append(field_name)

        criteria = CustomReportCriteria(
            column_list=columns,
            root_term=RawReportTerm(
                data_type_name, "RecordId", RawTermOperation.GREATER_THAN_OPERATOR, "0"
            ),
            root_data_type=data_type_name,
            page_size=100,
            page_number=0,
        )
        display_rows: List[dict[str, object]] = CustomReportUtil.run_custom_report(
            self.user, criteria, page_limit=1, page_size=100, page_number=0
        )
        if not display_rows:
            return []

        ro = AnyFieldInfo(editable=False, required=False, visible=True)
        fb = FieldBuilder(data_type_name)
        field_defs: List[object] = []
        for field_name in display_field_names:
            if field_name == "RecordId":
                field_defs.append(fb.long_field(field_name, abstract_info=ro, display_name=field_name))
            else:
                field_defs.append(fb.string_field(field_name, abstract_info=ro, display_name=field_name))

        try:
            selected_rows: List[dict[str, object]] = self.callback.selection_dialog(
                "Choose Existing Records",
                field_defs,
                display_rows,
                multi_select=True,
                shortcut_single_option=False,
            )
        except SapioUserCancelledException:
            return []
        if not selected_rows:
            return []

        record_ids: List[object] = [row["RecordId"] for row in selected_rows if row.get("RecordId") is not None]
        if not record_ids:
            return []

        page = self.dr_man.query_data_records(data_type_name, "RecordId", record_ids)
        return page.result_list or []

    def _create_table_entry_with_records(self, data_type_name: str, record_list: List[DataRecord] ) -> None:
        entry_name = "Created Records"
        models = self.inst_man.add_existing_records(record_list)
        ExperimentStepFactory(self.exp_handler).create_table_step(
            entry_name, data_type_name, records=models
        )
        self.rec_man.store_and_commit()