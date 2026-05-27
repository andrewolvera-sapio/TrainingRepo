from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult

from webhook_handler import SapioWebhookHandler

class HelloWorldButton(SapioWebhookHandler):

    """
    This button will display a client callback dialog with text "Hello World!". The intent of this plugin
    is to provide an example of a webhook.
    """

    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        return SapioWebhookResult(passed=True, display_text="Hello World!")

    