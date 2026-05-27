import traceback
from abc import abstractmethod
from logging import Logger
from typing import List

import waitress.utilities
from sapiopylib.rest.DataRecordManagerService import DataRecordManager
from sapiopylib.rest.ELNService import ElnManager
from sapiopylib.rest.WebhookService import AbstractWebhookHandler
from sapiopylib.rest.pojo.DataRecord import DataRecord
from sapiopylib.rest.pojo.webhook.WebhookContext import SapioWebhookContext
from sapiopylib.rest.pojo.webhook.WebhookResult import SapioWebhookResult
from sapiopylib.rest.utils.recordmodel.RecordModelManager import RecordModelManager, RecordModelInstanceManager

from sapio.recordmodel.data_type_models import DirectoryModel


class SapioWebhookHandler(AbstractWebhookHandler):
    """
    The class that all webhook endpoints should extend. Wraps the execute command in a try/except to capture
    any runtime errors and report them to the user.
    """
    logger: Logger = waitress.utilities.logger
    data_record_manager: DataRecordManager
    eln_manager: ElnManager
    exp_id: int
    rec_model_manager: RecordModelManager
    instance_manager: RecordModelInstanceManager

    def run(self, context: SapioWebhookContext) -> SapioWebhookResult:
        self.data_record_manager = context.data_record_manager
        self.eln_manager = context.eln_manager

        self.rec_model_manager = RecordModelManager(context.user)
        self.instance_manager = self.rec_model_manager.instance_manager

        if context.eln_experiment:
            self.exp_id = context.eln_experiment.notebook_experiment_id
        try:
            return self.execute(context)
        except Exception:
            self.logger.error("Error ({user:s}):\n{trc:s}"
                              .format(user=context.user.username, trc=traceback.format_exc()))
            return SapioWebhookResult(False, display_text="Error occurred during webhook execution.")

    @abstractmethod
    def execute(self, context: SapioWebhookContext) -> SapioWebhookResult:
        pass

    @staticmethod
    def getRootDirectory(context: SapioWebhookContext) -> DirectoryModel:
        # get root directory
        directories: List[DataRecord] = context.data_record_manager.query_data_records(
            DirectoryModel.DATA_TYPE_NAME,
            DirectoryModel.DIRECTORYNAME__FIELD_NAME.__str__(),
            ["Root Directory"]).result_list

        rootDirectory: DirectoryModel = None
        if directories:
            rootDirectory = RecordModelManager(context.user).instance_manager.add_existing_record_of_type(directories[0], DirectoryModel)
        return rootDirectory
