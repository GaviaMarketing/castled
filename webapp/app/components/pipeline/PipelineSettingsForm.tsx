import React from "react";
import { Form, Formik, FormikHelpers } from "formik";
import InputField from "@/app/components/forminputs/InputField";
import InputSelect from "@/app/components/forminputs/InputSelect";
import renderUtils from "@/app/common/utils/renderUtils";
import ButtonSubmit from "@/app/components/forminputs/ButtonSubmit";
import {
  ScheduleTimeUnit,
  ScheduleType,
  SchedulTimeUnitLabel,
} from "@/app/common/enums/ScheduleType";
import { QueryMode, QueryModeLabel } from "@/app/common/enums/QueryMode";
import { PipelineSchedule } from "@/app/common/dtos/PipelineCreateRequestDto";
import { Col, Row } from "react-bootstrap";
import pipelineScheduleUtils from "@/app/common/utils/pipelineScheduleUtils";

export interface PipelineSettingsProps {
  initialValues: PipelineSettingsConfig;
  submitLabel: string;
  onSubmit: (
    name: string,
    pipelineSchedule: PipelineSchedule,
    queryMode: QueryMode,
    setSubmitting: (isSubmitting: boolean) => void
  ) => void;
}

export interface PipelineSettingsConfig {
  name?: string;
  schedule: SettingSchedule;
  queryMode?: QueryMode;
}

export interface SettingSchedule {
  timeUnit?: ScheduleTimeUnit;
  frequency?: number;
}

function PipelineSettingsForm({
  initialValues,
  onSubmit,
  submitLabel,
}: PipelineSettingsProps) {
  const handleSubmit = (
    pipelineSettings: PipelineSettingsConfig,
    { setSubmitting }: FormikHelpers<any>
  ) => {
    setSubmitting(false); //NP: this is required when the first query responds with an error and the submit button should still be available for next query
    onSubmit(
      pipelineSettings.name!,
      {
        type: ScheduleType.FREQUENCY,
        frequency: pipelineScheduleUtils.getFrequencySecs(
          pipelineSettings.schedule.frequency!,
          pipelineSettings.schedule.timeUnit!
        ),
      },
      pipelineSettings.queryMode!,
      setSubmitting
    );
  };
  return (
    <Formik initialValues={initialValues} onSubmit={handleSubmit}>
      {({ values, setFieldValue, setFieldTouched, isSubmitting }) => (
        <Form>
          <InputField title="Pipeline Name" type="text" name="name" />
          <InputSelect
            title="Query Mode"
            options={renderUtils.selectOptions(QueryModeLabel)}
            values={values}
            setFieldValue={setFieldValue}
            setFieldTouched={setFieldTouched}
            name="queryMode"
          />
          <label className="form-label mb-3">Pipeline Schedule</label>
          <Row>
            <Col>
              <InputField
                title="Frequency"
                type="number"
                name="schedule.frequency"
                inputClassName="form-control-lg"
              />
            </Col>
            <Col>
              <InputSelect
                title="Time Unit"
                options={renderUtils.selectOptions(SchedulTimeUnitLabel)}
                values={values}
                setFieldValue={setFieldValue}
                setFieldTouched={setFieldTouched}
                name="schedule.timeUnit"
              />
            </Col>
          </Row>
          <ButtonSubmit submitting={isSubmitting}>{submitLabel}</ButtonSubmit>
        </Form>
      )}
    </Formik>
  );
}

export default PipelineSettingsForm;
