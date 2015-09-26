package edu.sintez.tasklist.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import edu.sintez.tasklist.R;
import edu.sintez.tasklist.model.AppContext;
import edu.sintez.tasklist.model.Priority;
import edu.sintez.tasklist.model.ToDoDocument;

import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * Создержание конкретной заметки
 */
public class ToDoDetail extends Activity {

	public static final String LOG = ToDoDetail.class.getName();
	public static final int NAME_LEN = 30;
	public static final String MSG_DEL_THIS_DOC = "Вы действительно хотите удалить эту заметку ?";
	public static final String MSG_DOC_IS_CHANGE_CONFIRM_SAVE = "Заметка была изменена, сохранить ?";
	public static final String MSG_DOC_NO_CHANGE = "В заметке не было изменений.";
	public static final String YES = "Да";
	public static final String CANCEL = "Отмена";
	public static final String NO = "Нет";
	public static final String DEFAULT_NAME = "New task";

	private ToDoDocument doc;
	private List<ToDoDocument> listDocs;

	private int typeAction;
	private int valDocIndex;

	private MenuItem menuPr;
	private Priority currPriority;

	private EditText etContent;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_detail);

		etContent = (EditText) findViewById(R.id.et);

		listDocs = ((AppContext) getApplicationContext()).getListDocs();
		for (ToDoDocument listDoc : listDocs) {
			Log.d(LOG, "doc name = " + listDoc.getName());
		}

		typeAction = getIntent().getExtras().getInt(AppContext.KEY_TYPE_ACTION);

		selDocAction(typeAction);
	}

	private void selDocAction(int action) {
		switch (action) {
			case AppContext.VAL_ACTION_NEWTASK:
				doc = new ToDoDocument();
				break;
			case AppContext.VAL_ACTION_UPDATE:
				valDocIndex = getIntent().getExtras().getInt(AppContext.KEY_DOC_INDEX);
				doc = listDocs.get(valDocIndex);
				etContent.setText(doc.getContent());
				break;
		}
		currPriority = doc.getPriority();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_todo_details, menu);

		menuPr = menu.findItem(R.id.menu_priority);
		MenuItem mi = menuPr.getSubMenu().getItem(doc.getPriority().getIndex());
		mi.setChecked(true);

//		return super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.item2_back:
				Log.d(LOG, "back");
				Log.d(LOG, "is change ? " + isChangeDoc());
				if (isChangeDoc()) {
					dialogConfirmSave();
				} else {
					finish();
				}
				return true;

			case R.id.item3_save:
				Log.d(LOG, "save");
				saveDocument();
				finish();
				return true;

			case R.id.item4_del:
				Log.d(LOG, "del");
				dialogConfirmDel();
				return true;

			case R.id.menu_pr_low:
			case R.id.menu_pr_medium:
			case R.id.menu_pr_high: {
				item.setChecked(true);
				Priority[] values = Priority.values();
				currPriority = values[Integer.parseInt(item.getTitleCondensed().toString())];
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void saveDocument() {
		SharedPreferences shp = getSharedPreferences(String.valueOf(doc.getCreateDate().getTime()), MODE_PRIVATE);
		SharedPreferences.Editor editor = shp.edit();

		switch (typeAction) {
			case AppContext.VAL_ACTION_NEWTASK:
				doc.setCreateDate(new Date());
				doc.setContent(etContent.getText().toString());
				doc.setName(getDocName());
				doc.setPriority(currPriority);

				editor.putString(AppContext.KEY_NAME, doc.getName());
				editor.putString(AppContext.KEY_CONTENT, doc.getContent());
				editor.putLong(AppContext.KEY_DATE, doc.getCreateDate().getTime());
				editor.putInt(AppContext.KEY_PRIORITY, doc.getPriority().getIndex());
				editor.commit();

				listDocs.add(doc);
				break;

			case AppContext.VAL_ACTION_UPDATE:
				if (isChangeDoc()) {
					String shpDirPath = getApplicationInfo().dataDir + "/" + ToDoList.SHARED_PREFS_DIR;
					File file = new File(shpDirPath, doc.getCreateDate() + ".xml");

					doc.setCreateDate(new Date());
					doc.setContent(etContent.getText().toString());
					doc.setName(getDocName());
					doc.setPriority(currPriority);

					editor.putString(AppContext.KEY_CONTENT, doc.getContent());
					editor.putString(AppContext.KEY_NAME, doc.getName());
					editor.putLong(AppContext.KEY_DATE, doc.getCreateDate().getTime());
					editor.putInt(AppContext.KEY_PRIORITY, doc.getPriority().getIndex());
					editor.commit();

					file.renameTo(new File(shpDirPath, doc.getCreateDate().getTime() + ".xml"));
				} else {
					Toast.makeText(this, MSG_DOC_NO_CHANGE, Toast.LENGTH_SHORT).show();
					finish();
				}
		}

	}

	private void deleteDocument(ToDoDocument doc){
		if (typeAction == AppContext.VAL_ACTION_UPDATE) {
			listDocs.remove(doc.getNumber());
			finish();
		}
	}

//	/**
//	 * Обновление индексов списка документов.
//	 * Нужно для правильной работы алгоритма фильтрации документов.
//	 */
//	private void updateIndices(){
//		ToDoDocument doc;
//		for (int i = 0; i < listDocs.size(); i++) {
//			doc = listDocs.get(i);
//			doc.setNumber(i);
//		}
//	}

	/**
	 * Проверка, редактировался ли документ. Проверятеся содержимое и приоритет
	 * @return true - да (изменения выполнялись) ; false - нет
	 */
	private boolean isChangeDoc() {
		if (etContent.getText().toString().equals(doc.getContent()) &&
				currPriority == doc.getPriority()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Получение имени документа. Имя берется из содержимого документа.
	 * Если содержимое документа более NAME_LEN символов
	 * тогда оно сокращается до NAME_LEN символов. Остальные символы заменяются троеточием.
	 * Если в содержимом нет ни одного символа, тогда в качестве имени возвращается DEFAULT_NAME.
	 * @return имя документа
	 */
	private String getDocName(){
		if (etContent.getText().toString().equals("")){
			return DEFAULT_NAME;
		}
		StringBuilder sb = new StringBuilder(etContent.getText());
		if (sb.length() > NAME_LEN){
			sb.delete(NAME_LEN, sb.length()).append("...");
		}
		String shortName = sb.toString().trim().split("\n")[0];
		return (shortName.length() > 0) ? shortName : doc.getName();
	}


	/**
	 * Вызов диалога подтверждения удаления заметки.
	 * Если пользователь ответил "Да" - выполняется удаление открытой заметки
	 * из списка и переход в главную активность. Если пользователь ответил "Отмена"
	 * ничего не происходит. Детальная активность остается открытой.
	 */
	private void dialogConfirmDel(){
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setMessage(MSG_DEL_THIS_DOC);

		adb.setPositiveButton(YES, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteDocument(doc);
				finish();
			}
		});
		adb.setNegativeButton(CANCEL, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog alertDialog = adb.create();
		alertDialog.show();

	}

	/**
	 * Вызов диалога подтверждения сохранения заметки.
	 * Если пользователь ответил "Да" - выполняется созранени открытой заметки
	 * и переход в главную активность. Если пользователь ответил "Нет" то
	 * заметка не сохраняется, выполняется переход в главную активность.
	 */
	private void dialogConfirmSave(){
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setMessage(MSG_DOC_IS_CHANGE_CONFIRM_SAVE);

		adb.setPositiveButton(YES, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveDocument();
				finish();
			}
		});
		adb.setNegativeButton(NO, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		AlertDialog alertDialog = adb.create();
		alertDialog.show();
	}

}
