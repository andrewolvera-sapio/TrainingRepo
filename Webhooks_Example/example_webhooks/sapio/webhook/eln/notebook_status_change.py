from __future__ import annotations

from sapiopylib.rest.ELNService import ElnExperiment
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.eln.SapioELNEnums import ElnExperimentStatus


class NotebookStatusChange(CommonsWebhookHandler):
    KEY: str = "AUTO COMPLETE FIRST ENTRY"
    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        exp: ElnExperiment = context.eln_experiment

        if exp is None or self.exp_handler is None:
            return SapioWebhookResult(True)
        if exp.notebook_experiment_status != ElnExperimentStatus.New:
            return SapioWebhookResult(True)
        
        entry_name: str = "Experiment Details"
        step = self.exp_handler.get_step(entry_name)
        entry_id: int = step.eln_entry.entry_id

        self.exp_handler.complete_step(entry_name)

        return SapioWebhookResult(True, directive=self.directive.eln_entry(
            exp.notebook_experiment_id,
            entry_id
        ))