package com.example.user.notficationmanager;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

/*
    QueuedNotification is a simple class to store notifications.
    The persistent storing was tested, but all Intents were broken, so single session queue is supported only.

*/
public class QueuedNotification {

    public Notification not;

    public void Set(StatusBarNotification sbn) {
        not = sbn.getNotification().clone();
    }

    public Notification Get() {
        return not;
    }

}
