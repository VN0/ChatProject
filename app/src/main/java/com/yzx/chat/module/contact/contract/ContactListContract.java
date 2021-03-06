package com.yzx.chat.module.contact.contract;

import com.yzx.chat.base.BasePresenter;
import com.yzx.chat.base.BaseView;
import com.yzx.chat.core.entity.ContactEntity;

import java.util.List;

/**
 * Created by YZX on 2017年11月19日.
 * 每一个不曾起舞的日子 都是对生命的辜负
 */


public class ContactListContract {

    public interface View extends BaseView<Presenter> {
        void updateUnreadBadge(int unreadCount);

        void showContactList(List<ContactEntity> contactList);

        void showTagCount(int count);
    }


    public interface Presenter extends BasePresenter<View> {
        void loadUnreadCount();

        void loadAllContact();

        void loadTagCount();

    }
}
