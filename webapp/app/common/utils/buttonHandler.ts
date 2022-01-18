import { StringAnyMap } from "@/app/common/utils/types";
import eventService from "@/app/services/eventService";

export interface ButtonMeta {
  id: string;
  dataLayer?: StringAnyMap;
}

function buttonHandler(
  isOss: boolean,
  buttonMeta: ButtonMeta,
  onClick?: () => void
) {
  return () => {
    if (!isOss) {
      eventService.send({
        event: buttonMeta.id + "_clicked",
        ...buttonMeta.dataLayer,
      });
    }
    onClick?.();
  };
}

export default buttonHandler;
