package com.example.user.notficationmanager;

import android.app.Notification;
import android.app.PendingIntent;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class QueuedNotification {
    //public byte[] data;
    //public String json_data;
    public Notification not;

    public void Set(StatusBarNotification sbn) {

        /*JSONObject jsonObject= new JSONObject();
        try {
            jsonObject.put("StatusBarNotification", sbn.);
            jsonObject.put("Notification", sbn.getNotification());
            data = jsonObject.toString();
            Log.i("QueuedNotification", data);

        } catch (JSONException e) {
            Log.i("QueuedNotification", "Setting JSON data from StatusBarNotification failed");
        }*/

        /*Notification not = sbn.getNotification();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(not.actions);
            //oos.writeObject(not.audioAttributes); // java.io.NotSerializableException
            oos.writeObject(not.category);
            oos.writeObject(not.color);
            oos.writeObject(not.contentIntent);
            oos.writeObject(not.defaults);
            oos.writeObject(not.deleteIntent);
            //oos.writeObject(not.extras); // java.io.NotSerializableException: android.os.Bundle
            oos.writeObject(not.flags);
            oos.writeObject(not.fullScreenIntent);
            oos.writeObject(not.iconLevel);
            oos.writeObject(not.icon);
            oos.writeObject(not.ledARGB);
            oos.writeObject(not.ledOffMS);
            oos.writeObject(not.ledOnMS);
            oos.writeObject(not.priority);
            //oos.writeObject(not.publicVersion); // java.io.NotSerializableException
            oos.writeObject(not.sound);
            oos.writeObject(not.tickerText);
            oos.writeObject(not.vibrate);
            oos.writeObject(not.visibility);
            oos.writeObject(not.when);

            data = baos.toByteArray();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/


        /*
        Gson gson = new Gson();
        json_data = gson.toJson(sbn.getNotification());
         */

        not = sbn.getNotification().clone();
    }

    public Notification Get() {
        /*try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            //return (Notification)ois.readObject();
            Notification not = new Notification();
            not.actions = (Notification.Action[])ois.readObject();
            //not.audioAttributes = ois.readObject();
            not.category = (String)ois.readObject();
            not.color = (int)ois.readObject();
            not.contentIntent = (PendingIntent)ois.readObject();
            not.defaults = (int)ois.readObject();
            not.deleteIntent = (PendingIntent)ois.readObject();
            //not.extras = ois.readObject();
            not.flags = (int)ois.readObject();
            not.fullScreenIntent = (PendingIntent)ois.readObject();
            not.iconLevel = (int)ois.readObject();
            not.icon = (int)ois.readObject();
            not.ledARGB = (int)ois.readObject();
            not.ledOffMS = (int)ois.readObject();
            not.ledOnMS = (int)ois.readObject();
            not.priority = (int)ois.readObject();
            //not.publicVersion = ois.readObject();
            not.sound = (Uri)ois.readObject();
            not.tickerText = (CharSequence)ois.readObject();
            not.vibrate = (long[])ois.readObject();
            not.visibility = (int)ois.readObject();
            not.when = (long)ois.readObject();
            return not;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        */

        /*
        Notification notification = gson.fromJson(json_data, Notification.class);
         */

        return not;
    }


}
