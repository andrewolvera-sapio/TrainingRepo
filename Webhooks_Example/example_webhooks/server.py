#! /usr/bin/env python

import urllib3
from sapiopylib.rest.WebhookService import WebhookConfiguration, WebhookServerFactory, AbstractWebhookHandler
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from waitress import serve

from sapio.webhook.button.hello_world_button import HelloWorldButton
from sapio.webhook.button.add_roles_button import AddRolesButton
from sapio.webhook.button.change_captain_button import ChangeCaptainButton
from sapio.webhook.button.sapio_word_button import SapioWordButton
from sapio.webhook.list_fill.contact_selection import SprintContactSelection
from sapio.webhook.on_save.sprint_on_save import SprintOnSave
from sapio.webhook.eln.notebook_status_change import NotebookStatusChange
from sapio.webhook.eln.notebook_add_records import NotebookAddRecords

urllib3.disable_warnings()

config: WebhookConfiguration = WebhookConfiguration(verify_sapio_cert=False, debug=True)


class Ping(AbstractWebhookHandler):
    def run(self, context: SapioWebhookContext) -> SapioWebhookResult:
        return SapioWebhookResult(True, "Success!")


# Ping text
config.register('/ping', Ping)

# Hello world webhook
config.register('/hello_world_button', HelloWorldButton)

# Add roles webhook
config.register('/add_roles_button', AddRolesButton)

# Change captain webhook
config.register('/change_captain_button', ChangeCaptainButton)

# Sapio word webhook
config.register('/sapio_word_button', SapioWordButton)

# Sprint contact selection webhook
config.register('/sprint_contact_selection', SprintContactSelection)

# Sprint on save webhook
config.register('/sprint_on_save', SprintOnSave)

# Notebook status change webhook
config.register('/notebook_status_change', NotebookStatusChange)

config.register('/notebook_add_records', NotebookAddRecords)

app = WebhookServerFactory.configure_flask_app(app=None, config=config)


# UNENCRYPTED! This should not be used in production. You should give the "app" a ssl_context or set up a reverse-proxy
# Dev Mode:
# app.run(host="0.0.0.0", port=8090)
# Production Mode
# serve(app, host="0.0.0.0", port=8090)
@app.route("/ping")
def ping():
    return "Pong!"


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8090)
    parser.add_argument("--host", type=str, default="0.0.0.0")
    parser.add_argument("--debug", type=bool, default=True)
    arg = parser.parse_args()
    if arg.debug:
        app.run(host=arg.host, port=arg.port, debug=True)
    else:
        serve(app, host=arg.host, port=arg.port)
