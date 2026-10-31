-keep class com.nsr.qilin.domain.model.** { *; }
-keep class com.nsr.qilin.data.service.DownloadModel { *; }
-keep class com.nsr.qilin.data.service.DownloadTask { *; }
-keep class com.nsr.qilin.data.service.AniListUser { *; }
-keep class com.nsr.qilin.data.service.AniListMedia { *; }
-keep class com.nsr.qilin.data.service.AniListMediaDetail { *; }
-keep class com.nsr.qilin.data.service.AniListEntry { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class * extends com.google.gson.** { *; }

# Commons Compress registers extra-field classes reflectively via Class.newInstance().
# R8 otherwise merges/abstracts them under minification, crashing ZipFile creation.
-keep class org.apache.commons.compress.archivers.zip.AsiExtraField { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X7875_NewUnix { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.JarMarker { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.UnicodePathExtraField { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.UnicodeCommentExtraField { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.Zip64ExtendedInformationExtraField { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X000A_NTFS { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X0014_X509Certificates { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X0015_CertificateIdForFile { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X0016_CertificateIdForCentralDirectory { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X0017_StrongEncryptionHeader { public <init>(); }
-keep class org.apache.commons.compress.archivers.zip.X0019_EncryptionRecipientCertificateList { public <init>(); }
