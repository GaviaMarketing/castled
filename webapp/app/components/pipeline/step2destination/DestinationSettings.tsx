import { PipelineWizardStepProps } from "@/app/components/pipeline/PipelineWizard";
import Layout from "@/app/components/layout/Layout";
import React, { useEffect, useState } from "react";
import appsService from "@/app/services/appsService";
import { usePipelineWizContext } from "@/app/common/context/pipelineWizardContext";
import { FormFieldsDto } from "@/app/common/dtos/FormFieldsDto";
import { Form, Formik } from "formik";
import DynamicFormFields from "@/app/components/connectors/DynamicFormFields";
import ButtonSubmit from "@/app/components/forminputs/ButtonSubmit";
import bannerNotificationService from "@/app/services/bannerNotificationService";
import Loading from "@/app/components/common/Loading";
import _ from "lodash";
import dynamicFormUtils from "@/app/common/utils/dynamicFormUtils";
import { removeAllLocalStorageMapping } from "../step3mapping/utils/MappingAutoFill";

const DestinationSettings = ({
  curWizardStep,
  steps,
  stepGroups,
  setCurWizardStep,
}: PipelineWizardStepProps) => {
  const { pipelineWizContext, setPipelineWizContext } = usePipelineWizContext();
  const [formFields, setFormFields] = useState<FormFieldsDto | undefined>();

  const type = pipelineWizContext?.appType?.value;
  useEffect(() => {
    if (!pipelineWizContext) return;
    const type = pipelineWizContext.appType?.value;
    if (!type) {
      setCurWizardStep("source", "selectType");
      return;
    }
    appsService
      .mappingFormFields(type)
      .then(({ data }) => setFormFields(data))
      .catch(() => {
        bannerNotificationService.error("Error loading form fields");
      });
  }, [pipelineWizContext?.appType?.value, type]);
  if (!pipelineWizContext) return <Loading />;
  return (
    <Layout
      title={steps[curWizardStep].title}
      subTitle="Configure settings for how the sync should happen to the destination"
      centerTitle={true}
      steps={steps}
      stepGroups={stepGroups}
    >
      <Formik
        key={`fields-${!!formFields}`}
        initialValues={dynamicFormUtils.getInitialValues(
          formFields,
          undefined,
          pipelineWizContext.values?.appSyncConfig as any
        )}
        onSubmit={({ values }) => {
          setCurWizardStep(undefined, "mapping");
        }}
        validateOnBlur={false}
        validate={(values) => {
          if (
            JSON.stringify(pipelineWizContext.values?.appSyncConfig.object) !==
            JSON.stringify(values.object)
          ) {
            removeAllLocalStorageMapping();
          }
          _.set(pipelineWizContext, "values.appSyncConfig", values);
          setPipelineWizContext(pipelineWizContext);
          return dynamicFormUtils.getValidationErrors(
            formFields,
            undefined,
            values,
            {}
          );
        }}
      >
        {({ setFieldValue, setFieldTouched, isSubmitting, isValid }) => (
          <div className="form-width-75">
            <Form>
              <DynamicFormFields
                setFieldValue={setFieldValue}
                setFieldTouched={setFieldTouched}
                formFields={formFields}
                values={pipelineWizContext.values?.appSyncConfig}
                dataFetcher={(optionsRef) =>
                  appsService.dynamicFieldValues(
                    optionsRef,
                    pipelineWizContext.values
                  )
                }
              />
              <ButtonSubmit submitting={isSubmitting}>Continue</ButtonSubmit>
            </Form>
          </div>
        )}
      </Formik>
    </Layout>
  );
};

export default DestinationSettings;
