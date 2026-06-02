from sapiopycommons.callbacks.callback_util import BlankResultHandling
from sapiopycommons.general.directive_util import TableDirective
from sapiopycommons.general.exceptions import SapioUserCancelledException
from sapiopycommons.webhook.webhook_handlers import CommonsWebhookHandler
from sapiopylib.rest.pojo.datatype.FieldDefinition import VeloxIntegerFieldDefinition
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult

class AddRolesButton(CommonsWebhookHandler):
    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        if not self.can_send_client_callback():
            return SapioWebhookResult(passed=False, display_text="Cannot use client callback")

        try:
            numRolesRaw = self.callback.input_dialog(
                "Create Records",
                "Enter the number of roles to add:", 
                VeloxIntegerFieldDefinition(
                    "TempWebhookInput",
                    "RecordCount",
                    "Number of Records",
                    min_value=1,
                    max_value=100,
                    editable=True
                ),
                blank_result_handling=BlankResultHandling.REPEAT
                )
        except SapioUserCancelledException:
            return SapioWebhookResult(True)
        
        numRoles = int(numRolesRaw)

        roles = self.inst_man.add_new_records("DeveloperRole", numRoles)
        self.rec_man.store_and_commit()

        return SapioWebhookResult(True, directive=self.directive.record_table([x.get_data_record() for x in roles]))