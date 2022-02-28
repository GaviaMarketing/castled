import React from "react";
import cn from "classnames";
import { FieldAttributes } from "formik";
import { IconPlayerPlay, IconLoader } from "@tabler/icons";

interface ButtonSubmitProps extends FieldAttributes<any> {
  children?: JSX.Element | any | null;
  icon?: JSX.Element | string | null;
}

const ButtonSubmit = ({
  submitting,
  children,
  icon,
  ...props
}: ButtonSubmitProps) => {
  if (props.connectorType == "FBCUSTOMAUDIENCE") {
    return (
      <button
        type="submit"
        {...props}
        disabled={submitting}
        className={cn("mt-2 btn ext-logo-btn btn-shadow", props.className)}
      >
        <img src="/images/fb-btn.png" alt="Login with Facebook" />
        {submitting === true ? <IconLoader className="spinner-icon" /> : ""}
      </button>
    );
  } else if (props.connectorType == "GOOGLEADS") {
    return (
      <button
        type="submit"
        {...props}
        disabled={submitting}
        className={cn("mt-2 btn ext-logo-btn btn-shadow", props.className)}
      >
        <img src="/images/google-btn.png" alt="Login with Google" />
        {submitting === true ? <IconLoader className="spinner-icon" /> : ""}
      </button>
    );
  } else {
    return (
      <button
        type="submit"
        {...props}
        disabled={submitting}
        className={cn("mt-2 btn btn-primary btn-shadow", props.className)}
      >
        {children === "Run Query" ? (
          <IconPlayerPlay size={14} style={{ marginRight: "5px" }} />
        ) : (
          ""
        )}
        {children || "Submit"}
        {submitting === true ? <IconLoader className="spinner-icon" /> : ""}
      </button>
    );
  }
};
export default ButtonSubmit;
