package com.yzx.chat.module.common.contract;

import com.yzx.chat.base.BasePresenter;
import com.yzx.chat.base.BaseView;
import com.yzx.chat.core.entity.UserEntity;

/**
 * Created by YZX on 2018年06月12日.
 * 每一个不曾起舞的日子 都是对生命的辜负
 */
public class QrCodeScanContract {

    public interface View extends BaseView<Presenter> {

        void startStrangerProfileActivity(UserEntity user);

        void startContactProfileActivity(String contactID);

        void startGroupChatActivity(String groupID);
    }


    public interface Presenter extends BasePresenter<View> {

        void decodeQRCodeContent(String content, boolean isAlreadyDeciphered);

        void decodeQRCodeContentFromFile(String filePath);
    }
}
