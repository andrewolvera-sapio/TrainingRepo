from typing import Optional

from sapiopylib.rest.pojo.datatype.FieldDefinition import FieldType
from sapiopylib.rest.utils.recordmodel.RecordModelWrapper import WrappedRecordModel, WrapperField


class DirectoryModel(WrappedRecordModel):
    """
    Auto-Generated Record Model Wrapper for data type Directory
    Data Type Display Name: Directory (Directories)
    Fields: DirectoryName
    """
    DATA_TYPE_NAME: str = 'Directory'
    DIRECTORYNAME__FIELD_NAME: WrapperField = WrapperField("DirectoryName", FieldType.STRING)

    @classmethod
    def get_wrapper_data_type_name(cls):
        return cls.DATA_TYPE_NAME

    def set_DirectoryName_field(self, value: Optional[str]):
        """
        Set data field with field name 'DirectoryName' on this record model
        """
        self.set_field_value(self.DIRECTORYNAME__FIELD_NAME.field_name, value)

    def get_DirectoryName_field(self) -> Optional[str]:
        """
        Get data field value with field name 'DirectoryName' from this record model
        """
        return self.get_field_value(self.DIRECTORYNAME__FIELD_NAME.field_name)

