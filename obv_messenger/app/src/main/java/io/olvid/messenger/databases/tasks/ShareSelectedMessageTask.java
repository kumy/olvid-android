/*
 *  Olvid for Android
 *  Copyright © 2019-2026 Olvid SAS
 *
 *  This file is part of Olvid for Android.
 *
 *  Olvid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  Olvid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with Olvid.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.olvid.messenger.databases.tasks;


import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.olvid.messenger.R;
import io.olvid.messenger.databases.AppDatabase;
import io.olvid.messenger.databases.dao.FyleMessageJoinWithStatusDao;
import io.olvid.messenger.databases.entity.Message;

public class ShareSelectedMessageTask implements Runnable {
    private final WeakReference<FragmentActivity> activityWeakReference;
    private final Long selectedMessageId;


    public ShareSelectedMessageTask(FragmentActivity activity, Long selectedMessageId) {
        activityWeakReference = new WeakReference<>(activity);
        this.selectedMessageId = selectedMessageId;
    }

    @Override
    public void run() {
        AppDatabase db = AppDatabase.getInstance();
        Message message = db.messageDao().get(selectedMessageId);

        if (message != null) {
            if ((message.messageType != Message.TYPE_OUTBOUND_MESSAGE
                    && message.messageType != Message.TYPE_INBOUND_MESSAGE)
                    || message.wipeStatus != Message.WIPE_STATUS_NONE
                    || message.limitedVisibility
                    || message.isPollMessage()) {
                return;
            }

            Intent intent = new Intent();
            String mimeType = null;
            boolean hasText = message.contentBody != null && !message.contentBody.isEmpty();
            boolean multiple = (hasText && (message.totalAttachmentCount > 0)) || (message.totalAttachmentCount > 1);
            if (hasText) {
                intent.putExtra(Intent.EXTRA_TEXT, message.contentBody);
                mimeType = "text/plain";
            }
            boolean sharedAttachment = false;
            if (message.hasAttachments()) {
                List<FyleMessageJoinWithStatusDao.FyleAndStatus> fyleAndStatuses = db.fyleMessageJoinWithStatusDao().getCompleteFylesAndStatusForMessageSyncWithoutLinkPreview(message.id);
                if (multiple) {
                    ArrayList<Uri> uris = new ArrayList<>(fyleAndStatuses.size());
                    ArrayList<String> mimes = new ArrayList<>(fyleAndStatuses.size());
                    for (FyleMessageJoinWithStatusDao.FyleAndStatus fyleAndStatus : fyleAndStatuses) {
                        Uri uri = fyleAndStatus.getContentUriForExternalSharing();
                        // skip incomplete fyles whose sha256 isn't computed yet — sharing a null Uri
                        // breaks Sharesheet thumbnails and target apps receive an unusable stream
                        if (uri == null) continue;
                        String fyleMime = fyleAndStatus.fyleMessageJoinWithStatus.getNonNullMimeType();
                        uris.add(uri);
                        mimes.add(fyleMime);
                        mimeType = mimeGcd(mimeType, fyleMime);
                    }
                    if (!uris.isEmpty()) {
                        // ClipData per-URI carries per-item mime so receivers can inspect ClipDescription
                        // for the correct type instead of falling back to the GCD intent type. ClipData +
                        // FLAG_GRANT_READ_URI_PERMISSION is what lets the system Sharesheet preview and
                        // the chosen receiver actually read the URI.
                        ClipData clipData = new ClipData(new ClipDescription(null, mimes.toArray(new String[0])), new ClipData.Item(uris.get(0)));
                        for (int i = 1; i < uris.size(); i++) {
                            clipData.addItem(new ClipData.Item(uris.get(i)));
                        }
                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                        intent.setClipData(clipData);
                        sharedAttachment = true;
                    }
                } else {
                    FyleMessageJoinWithStatusDao.FyleAndStatus fyleAndStatus = fyleAndStatuses.get(0);
                    Uri uri = fyleAndStatus.getContentUriForExternalSharing();
                    if (uri != null) {
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        mimeType = fyleAndStatus.fyleMessageJoinWithStatus.getNonNullMimeType();
                        intent.setClipData(new ClipData(new ClipDescription(null, new String[]{ mimeType }), new ClipData.Item(uri)));
                        sharedAttachment = true;
                    }
                }
            }
            // No shareable attachment (e.g. the only attachment isn't downloaded yet). Still share
            // the text alone — matches legacy behaviour — but if there's no text either, bail out.
            if (!sharedAttachment && !hasText) {
                return;
            }
            if (multiple && sharedAttachment) {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            } else {
                intent.setAction(Intent.ACTION_SEND);
            }
            intent.setType(mimeType);
            // Required: without this flag the system Sharesheet and the chosen target app cannot
            // read the URI(s) and the thumbnail preview falls back to a generic file icon.
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            FragmentActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.title_sharing_chooser)));
            }
        }
    }

    private static String mimeGcd(String mimeType1, String mimeType2) {
        if (mimeType1 == null) {
            return mimeType2;
        }
        if (mimeType2 == null) {
            return mimeType1;
        }
        if (mimeType1.equals(mimeType2)) {
            return mimeType1;
        }
        String prefix = mimeType1.split("/")[0];
        if (mimeType2.split("/")[0].equals(prefix)) {
            return prefix + "/*";
        }
        return "*/*";
    }
}
