package com.waph1.markit.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NoteDao_Impl implements NoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NoteEntity> __insertionAdapterOfNoteEntity;

  private final EntityDeletionOrUpdateAdapter<NoteEntity> __updateAdapterOfNoteEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteNoteByPath;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfUpdateColor;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePinStatus;

  private final SharedSQLiteStatement __preparedStmtOfArchiveNote;

  private final SharedSQLiteStatement __preparedStmtOfTrashNote;

  private final SharedSQLiteStatement __preparedStmtOfRestoreNote;

  public NoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNoteEntity = new EntityInsertionAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notes` (`filePath`,`fileName`,`folder`,`title`,`contentPreview`,`content`,`lastModifiedMs`,`color`,`isPinned`,`isArchived`,`isTrashed`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindString(1, entity.getFilePath());
        statement.bindString(2, entity.getFileName());
        statement.bindString(3, entity.getFolder());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getContentPreview());
        statement.bindString(6, entity.getContent());
        statement.bindLong(7, entity.getLastModifiedMs());
        statement.bindLong(8, entity.getColor());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isArchived() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.isTrashed() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
      }
    };
    this.__updateAdapterOfNoteEntity = new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notes` SET `filePath` = ?,`fileName` = ?,`folder` = ?,`title` = ?,`contentPreview` = ?,`content` = ?,`lastModifiedMs` = ?,`color` = ?,`isPinned` = ?,`isArchived` = ?,`isTrashed` = ? WHERE `filePath` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindString(1, entity.getFilePath());
        statement.bindString(2, entity.getFileName());
        statement.bindString(3, entity.getFolder());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getContentPreview());
        statement.bindString(6, entity.getContent());
        statement.bindLong(7, entity.getLastModifiedMs());
        statement.bindLong(8, entity.getColor());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isArchived() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.isTrashed() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindString(12, entity.getFilePath());
      }
    };
    this.__preparedStmtOfDeleteNoteByPath = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM notes WHERE filePath = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM notes";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateColor = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET color = ? WHERE filePath = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePinStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isPinned = ? WHERE filePath = ?";
        return _query;
      }
    };
    this.__preparedStmtOfArchiveNote = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isArchived = 1, isTrashed = 0 WHERE filePath = ?";
        return _query;
      }
    };
    this.__preparedStmtOfTrashNote = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isTrashed = 1 WHERE filePath = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreNote = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isArchived = 0, isTrashed = 0 WHERE filePath = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNoteEntity.insert(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertNotes(final List<NoteEntity> notes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNoteEntity.insert(notes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNoteEntity.handle(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNoteByPath(final String filePath,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteNoteByPath.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteNoteByPath.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateColor(final String filePath, final long color,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateColor.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, color);
        _argIndex = 2;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateColor.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePinStatus(final String filePath, final boolean isPinned,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePinStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isPinned ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdatePinStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object archiveNote(final String filePath, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfArchiveNote.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfArchiveNote.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object trashNote(final String filePath, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfTrashNote.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfTrashNote.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreNote(final String filePath, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreNote.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, filePath);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRestoreNote.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteEntity>> getAllActiveNotes() {
    final String _sql = "SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, lastModifiedMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFolder = CursorUtil.getColumnIndexOrThrow(_cursor, "folder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsTrashed = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrashed");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFolder;
            _tmpFolder = _cursor.getString(_cursorIndexOfFolder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final long _tmpColor;
            _tmpColor = _cursor.getLong(_cursorIndexOfColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_1 != 0;
            final boolean _tmpIsTrashed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTrashed);
            _tmpIsTrashed = _tmp_2 != 0;
            _item = new NoteEntity(_tmpFilePath,_tmpFileName,_tmpFolder,_tmpTitle,_tmpContentPreview,_tmpContent,_tmpLastModifiedMs,_tmpColor,_tmpIsPinned,_tmpIsArchived,_tmpIsTrashed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getTrashedNotes() {
    final String _sql = "SELECT * FROM notes WHERE isTrashed = 1 ORDER BY lastModifiedMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFolder = CursorUtil.getColumnIndexOrThrow(_cursor, "folder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsTrashed = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrashed");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFolder;
            _tmpFolder = _cursor.getString(_cursorIndexOfFolder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final long _tmpColor;
            _tmpColor = _cursor.getLong(_cursorIndexOfColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_1 != 0;
            final boolean _tmpIsTrashed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTrashed);
            _tmpIsTrashed = _tmp_2 != 0;
            _item = new NoteEntity(_tmpFilePath,_tmpFileName,_tmpFolder,_tmpTitle,_tmpContentPreview,_tmpContent,_tmpLastModifiedMs,_tmpColor,_tmpIsPinned,_tmpIsArchived,_tmpIsTrashed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getArchivedNotes() {
    final String _sql = "SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY lastModifiedMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFolder = CursorUtil.getColumnIndexOrThrow(_cursor, "folder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsTrashed = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrashed");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFolder;
            _tmpFolder = _cursor.getString(_cursorIndexOfFolder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final long _tmpColor;
            _tmpColor = _cursor.getLong(_cursorIndexOfColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_1 != 0;
            final boolean _tmpIsTrashed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTrashed);
            _tmpIsTrashed = _tmp_2 != 0;
            _item = new NoteEntity(_tmpFilePath,_tmpFileName,_tmpFolder,_tmpTitle,_tmpContentPreview,_tmpContent,_tmpLastModifiedMs,_tmpColor,_tmpIsPinned,_tmpIsArchived,_tmpIsTrashed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getNotesByFolder(final String folder) {
    final String _sql = "SELECT * FROM notes WHERE folder = ? AND isTrashed = 0 ORDER BY isPinned DESC, lastModifiedMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, folder);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFolder = CursorUtil.getColumnIndexOrThrow(_cursor, "folder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsTrashed = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrashed");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFolder;
            _tmpFolder = _cursor.getString(_cursorIndexOfFolder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final long _tmpColor;
            _tmpColor = _cursor.getLong(_cursorIndexOfColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_1 != 0;
            final boolean _tmpIsTrashed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTrashed);
            _tmpIsTrashed = _tmp_2 != 0;
            _item = new NoteEntity(_tmpFilePath,_tmpFileName,_tmpFolder,_tmpTitle,_tmpContentPreview,_tmpContent,_tmpLastModifiedMs,_tmpColor,_tmpIsPinned,_tmpIsArchived,_tmpIsTrashed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getNoteByPath(final String filePath,
      final Continuation<? super NoteEntity> $completion) {
    final String _sql = "SELECT * FROM notes WHERE filePath = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, filePath);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NoteEntity>() {
      @Override
      @Nullable
      public NoteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFolder = CursorUtil.getColumnIndexOrThrow(_cursor, "folder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsTrashed = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrashed");
          final NoteEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFolder;
            _tmpFolder = _cursor.getString(_cursorIndexOfFolder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final long _tmpColor;
            _tmpColor = _cursor.getLong(_cursorIndexOfColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_1 != 0;
            final boolean _tmpIsTrashed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTrashed);
            _tmpIsTrashed = _tmp_2 != 0;
            _result = new NoteEntity(_tmpFilePath,_tmpFileName,_tmpFolder,_tmpTitle,_tmpContentPreview,_tmpContent,_tmpLastModifiedMs,_tmpColor,_tmpIsPinned,_tmpIsArchived,_tmpIsTrashed);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<String>> getAllLabels() {
    final String _sql = "SELECT DISTINCT folder FROM notes WHERE isTrashed = 0 AND isArchived = 0 AND folder != 'Inbox'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
