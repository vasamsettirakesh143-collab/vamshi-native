package com.vamshi.ai;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactLookupUtil {

    public static class Contact {
        public final String name;
        public final String number;

        public Contact(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    public static List<Contact> getAllContacts(Context context) {
        List<Contact> contacts = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            },
            null, null, null
        );

        if (cursor != null) {
            int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIdx);
                String number = cursor.getString(numberIdx);
                if (name != null && number != null) {
                    contacts.add(new Contact(name, number));
                }
            }
            cursor.close();
        }

        return contacts;
    }

    // Given something the user said (e.g. "mom", "rakesh brother"),
    // find the contact whose real saved name best matches it.
    public static Contact findBestMatch(Context context, String spokenName) {
        String query = spokenName.toLowerCase(Locale.US).trim();
        if (query.isEmpty()) return null;

        List<Contact> contacts = getAllContacts(context);

        Contact bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (Contact c : contacts) {
            String name = c.name.toLowerCase(Locale.US);

            if (name.equals(query)) {
                return c;
            }

            if (name.contains(query) || query.contains(name)) {
                int score = Math.abs(name.length() - query.length());
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = c;
                }
            }
        }

        return bestMatch;
    }
}
