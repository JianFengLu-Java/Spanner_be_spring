package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupSettingsDetailVO {
    private GroupProfileVO groupProfile;
    private GroupUserSettingsVO mySettings;
    private GroupAnnouncementVO latestAnnouncement;
    private GroupMediaOverviewVO mediaOverview;
}
