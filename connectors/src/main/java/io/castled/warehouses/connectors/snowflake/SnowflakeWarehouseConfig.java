package io.castled.warehouses.connectors.snowflake;

import com.amazonaws.regions.Regions;
import io.castled.OptionsReferences;
import io.castled.forms.*;
import io.castled.warehouses.BaseWarehouseConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnowflakeWarehouseConfig extends BaseWarehouseConfig {

    @FormField(description = "URL Prefix https://<account_name>.snowflake-computing.com", title = "Account Name", placeholder = "e.g. ab12345.us-east-2.aws", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String accountName;

    @FormField(title = "Warehouse Name", placeholder = "e.g. COMPUTE_WH", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String warehouseName;

    @FormField(title = "Database Name", placeholder = "e.g. demo_db", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String dbName;

    @FormField(title = "Database User", placeholder = "e.g. db_user", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String dbUser;

    @FormField(title = "Database Password", schema = FormFieldSchema.STRING, type = FormFieldType.PASSWORD)
    private String dbPassword;

    @FormField(title = "Schema Name", placeholder = "e.g. demo_schema", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String schemaName;

    @FormField(title = "S3 Bucket to be used as the staging area", placeholder = "e.g. demo-bucket", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String s3Bucket;

    @FormField(title = "S3 Access Key Id", placeholder = "e.g. AKIAIOSFODNN7EXAMPLE", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String accessKeyId;

    @FormField(title = "S3 Access Key Secret", placeholder = "e.g. wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", schema = FormFieldSchema.STRING, type = FormFieldType.TEXT_BOX)
    private String accessKeySecret;

    @FormField(title = "S3 Bucket Location", schema = FormFieldSchema.ENUM,
            type = FormFieldType.DROP_DOWN, optionsRef = @OptionsRef(value = OptionsReferences.AWS_REGIONS, type = OptionsRefType.STATIC))
    private Regions region;

    public String getDbHost() {
        return getAccountName() + ".snowflakecomputing.com";
    }

    public int getDbPort() {
        //https port
        return 443;
    }
}
