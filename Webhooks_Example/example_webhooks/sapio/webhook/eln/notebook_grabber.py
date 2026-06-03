from __future__ import annotations

from sapiopycommons.general.aliases import ElnEntryStep
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.ELNService import AbstractElnEntryCriteria, AbstractVeloxFieldDefinition, ElnBaseDataType, ElnExperiment, ExperimentEntry
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopycommons.eln.experiment_step_factory import ElnFormEntryCriteria, ExperimentStepFactory
from sapiopylib.rest.pojo.eln.field_set import ElnFieldSetInfo

from ast import List

class NotebookGrabber(CommonsWebhookHandler):
    entry_name: str = "Instrument Tracking Field Set"
    field_set_name: str = "Instrument Tracking"
    data_type_name: str = "InstrumentTracking"

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        exp: ElnExperiment | None = context.eln_experiment

        if not exp:
            return SapioWebhookResult(False)
        
        existing: ElnEntryStep | None = self.exp_handler.get_step(self.entry_name, False)
        if existing is not None:
            return SapioWebhookResult(False, client_callback_request=self.callback.display_error("Intrument Tracking Field Set Already Created"))
        
        field_set_info_list: List[ElnFieldSetInfo] = self.eln_man.get_field_set_info_list()
        instrument_tracking_field_set: ElnFieldSetInfo | None = self._field_set_info_list_search(field_set_info_list)
        if instrument_tracking_field_set is None:
            return SapioWebhookResult(False, client_callback_request=self.callback.display_error("Instrument Tracking Field Set Not Found"))
        
        entry_criteria: ElnFormEntryCriteria = ElnFormEntryCriteria(self.entry_name, ElnBaseDataType.EXPERIMENT_DETAIL.data_type_name, 3)
        entry_criteria.field_set_id_list = [instrument_tracking_field_set.field_set_id]
        # fields: list[AbstractVeloxFieldDefinition] = self.eln_man.get_predefined_fields_from_field_set_id(instrument_tracking_field_set.field_set_id)
        # entry_criteria.data_field_name_list = [field.data_field_name for field in fields]
        entry: ExperimentEntry = self.eln_man.add_experiment_entry(exp.notebook_experiment_id, entry_criteria)
    
        return SapioWebhookResult(True)

    def _field_set_info_list_search(self, field_set_info_list: List[ElnFieldSetInfo]) -> ElnFieldSetInfo | None:
        for field_set_info in field_set_info_list:
            if field_set_info.field_set_name == self.field_set_name:
                return field_set_info
        return None