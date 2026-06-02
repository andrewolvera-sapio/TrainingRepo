from __future__ import annotations

from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler

class NotebookStatusChange(CommonsWebhookHandler):
    KEY: str = "AUTO COMPLETE FIRST ENTRY"
    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        exp = context.eln_experiment
        if exp.status != "New":
            return SapioWebhookResult(True)
        
        entry_name: str = exp.getNotebookExperimentOptionMap().get(self.KEY)
        entry = self.exp_handler.

        entry.setEntryStatus("Completed")
        entry.setSubmittedBy(context.user.name)
        entry.setSubmittedDate(context.now)
        exp.setExperimentEntry(entry)

        return SapioWebhookResult(True, entry=entry)