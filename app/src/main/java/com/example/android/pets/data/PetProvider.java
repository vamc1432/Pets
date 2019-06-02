package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


public class PetProvider extends ContentProvider {

    private static final String LOG_TAG = PetProvider.class.getSimpleName();

    private PetDbHelper mDbHelper;

    private static final int PETS = 100;

    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PETS_PATH,PETS);

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PETS_PATH + "/#",PET_ID);

    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                cursor = db.query(PetContract.PetEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;

            case PET_ID:
                selection = PetContract.PetEntry.COLUMN_ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = db.query(PetContract.PetEntry.TABLE_NAME,projection,selection,selectionArgs,null,null,sortOrder);
                break;
            default:
                throw new IllegalArgumentException("cannot query unknown uri" + uri);

        }

        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                return insertPet(uri,contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported" + uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues values) {

        String Petname = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);

        if(Petname == null){
            throw new IllegalArgumentException("pet name can not be null");
        }

        Integer petgender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);

        if (petgender == null || !PetContract.PetEntry.isValidGender(petgender)){
            throw new IllegalArgumentException("pet gender is not valid");
        }
        Integer petWeight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);

        if (petWeight != null && petWeight < 0){
            throw new IllegalArgumentException("pet weight is inavlid");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(PetContract.PetEntry.TABLE_NAME,null,values);

        if (id < 0 ){
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri,null);

       return ContentUris.withAppendedId(uri,id);
    }

    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

      final int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                return updatepet(uri,contentValues,selection,selectionArgs);

            case PET_ID:
                selection = PetContract.PetEntry.COLUMN_ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatepet(uri,contentValues,selection,selectionArgs);
            default:
                throw new IllegalArgumentException("cannot update " + uri);

        }
    }

    private int updatepet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            Integer petgender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);

            if (petgender == null || !PetContract.PetEntry.isValidGender(petgender)) {
                throw new IllegalArgumentException("pet gender is not valid");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires a weight");
            }
        }

        if (values.size() == 0){
            return 0;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int rowsupdated = db.update(PetContract.PetEntry.TABLE_NAME,values,selection,selectionArgs);

        if (rowsupdated != 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsupdated;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        int rowsDeleted;

        switch (match) {
            case PETS:
                rowsDeleted = db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PET_ID:
                selection = PetContract.PetEntry.COLUMN_ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("cannot delete" + uri);
        }

        if (rowsDeleted != 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsDeleted;
    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

}
