import { FieldAttributes } from "formik";

export interface InputBaseProps extends FieldAttributes<any> {
  name: string;
  title: string | undefined;
  description?: string;
  className?: string;
  required?: boolean;
  onChange?: (value: string) => void;
}
