from __future__ import annotations

from sapiopycommons.general.aliases import ElnEntryStep
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopycommons.eln.experiment_step_factory import ExperimentStepFactory

class NotebookGrabber(CommonsWebhookHandler):
    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        exp = context.eln_experiment
        if not exp:
            return SapioWebhookResult(False)

        entry_name: str = "Instrument Tracking Field Set"
        data_type_name: str = "InstrumentTracking"
        existing: ElnEntryStep | None = self.exp_handler.get_step(entry_name, False)
        if existing is not None:
            return SapioWebhookResult(False, client_callback_request=self.callback.display_error("Intrument Tracking Field Set Already Created"))

        ExperimentStepFactory(self.exp_handler).create_form_step(
            entry_name, data_type_name
        )

        return SapioWebhookResult(True)