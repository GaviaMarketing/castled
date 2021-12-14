import React from "react";
import { useField } from "formik";
import { InputBaseProps } from "@/app/common/dtos/InputBaseProps";

const InputCheckbox = ({
  title,
  required,
  description,
  isValid,
  ...props
}: InputBaseProps) => {
  const [field, meta] = useField({ ...(props as any), type: "checkbox" });
  return (
    <div className="mb-3">
      <label className="checkbox form-label">
        <input
          {...field}
          {...(props as any)}
          type="checkbox"
          className="me-2"
        />
        {title}
        {required && "*"}
      </label>
      {(meta.touched || !isValid) && meta.error ? (
        <div className="error" style={{ color: 'red', fontSize: '12px'}}>{meta.error}</div>
      ) : null}
    </div>
  );
};
export default InputCheckbox;