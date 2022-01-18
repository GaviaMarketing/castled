import { StringAnyMap } from "@/app/common/utils/types";
import http from "@/app/services/http";
import tagManager from "react-gtm-module";
import { LoggedInUserDto } from "@/app/common/dtos/LoggedInUserDto";

export default {

  load: (user: LoggedInUserDto | null | undefined) => {
    let userProps = {};
    if (user) {
      userProps = {
        userId: user.id,
        userEmailId: user.email,
        userRole: user.role,
        userTeamId: user.teamId,
        userDomain: user.email.substring(user.email.indexOf("@") + 1),
        userFullName: user.name,
        userAvatar: user.avatar,
        userCreatedTs: user.createdTs,
      };
    }
    if (process.browser) {
      console.log("loading tag manager");
      tagManager.initialize({
        gtmId: "GTM-5PMX6V3",
        dataLayer: userProps,
      });
      console.log("GTM Loaded", userProps);
    }
  },
  send: (props: StringAnyMap) => {
    if (process.browser) {
      tagManager.dataLayer({ dataLayer: { ...props } });
      http.post("/v1/tracking", props)
      .catch(() => {
        console.log(`tracking failed for event ${props.event}`)
      });
    }
  },
};
