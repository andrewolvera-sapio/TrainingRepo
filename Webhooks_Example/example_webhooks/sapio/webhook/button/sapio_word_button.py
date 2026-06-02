from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopycommons.callbacks.callback_util import BlankResultHandling
from sapiopycommons.general.exceptions import SapioUserCancelledException

class SapioWordButton(CommonsWebhookHandler):
    def execute(self, context: SapioWebhookContext):
        sapio_word_list_config = self.list_man.get_picklist("Sapio Words")
        sapio_word_list = sapio_word_list_config.entry_list if sapio_word_list_config else []

        try:
            selection = self.callback.list_dialog(
                "SapioWord", 
                sapio_word_list, False,
                blank_result_handling=BlankResultHandling.DEFAULT
                )
        except SapioUserCancelledException:
            return SapioWebhookResult(True)
        
        currRecord = context.data_record

        currRecord.set_field_value("SapioWord", selection[0])
        context.data_record_manager.commit_data_records([currRecord])

        return SapioWebhookResult(True)