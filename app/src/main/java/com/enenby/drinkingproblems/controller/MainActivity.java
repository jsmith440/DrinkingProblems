package com.enenby.drinkingproblems.controller;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.enenby.drinkingproblems.OptionsMenu;
import com.enenby.drinkingproblems.R;
import com.enenby.drinkingproblems.ScreenLock;
import com.enenby.drinkingproblems.controller.QuestionsFragment.PhoneUnlockedReceiver;
import com.enenby.drinkingproblems.model.db.QuestionsDatabase;
import com.enenby.drinkingproblems.model.entity.Question;
import java.util.List;


/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements QuestionsFragment.QuestionLoader {


  /**
   * The constant QUESTION_AND_ANSWER.
   */
  public static final String QUESTION_AND_ANSWER = "QuestionAndAnswer";
  /**
   * The constant QUESTION_ID.
   */
  public static final String QUESTION_ID = "QuestionId";
  private TextView questionText;
  private int correct;
  private Toolbar topToolbar;
  private OnClickListener listener;
  private QuestionsDatabase database;
  private DevicePolicyManager devicePolicyManager;
  private ComponentName compName;
  /**
   * The constant RESULT_ENABLE.
   */
  public static final int RESULT_ENABLE = 11;
  private static long lastLockedTime = 0;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
    compName = new ComponentName(this, ScreenLock.class);
    registerReceiver(new PhoneUnlockedReceiver(), new IntentFilter("android.intent.action.USER_PRESENT"));

    topToolbar = findViewById(R.id.toolbar);
    setSupportActionBar(topToolbar);

    database = QuestionsDatabase.getInstance(this);


    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
        "App needs permission to lock screen");
    startActivityForResult(intent, RESULT_ENABLE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    new InitializeDataBase().execute();
    switch (requestCode) {
      case RESULT_ENABLE:
        if (resultCode == Activity.RESULT_OK) {
          Toast.makeText(MainActivity.this,
              "You have enabled the Admin Device Features",
              Toast.LENGTH_LONG).show();



        } else {
          Toast.makeText(MainActivity.this,
              "Cannot enable Admin Device Features",
              Toast.LENGTH_LONG).show();
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled = true;
    Fragment fragment = null;

    int id = item.getItemId();

    if (id == R.id.more_vert) {
      FragmentManager fm = getSupportFragmentManager();
      Fragment fragmentOptions = new OptionsMenu();

      fm.beginTransaction().add(R.id.fragment_container_options, fragmentOptions).commit();

    } else if (id == R.id.arrow_back) {
      Toast.makeText(MainActivity.this, "You pressed the back button! Hurray!", Toast.LENGTH_LONG).show();
    }
    //TODO make tool bar buttons navigate to where they should go
    return super.onOptionsItemSelected(item);
  }


  @Override
  protected void onResume() {
    super.onResume();
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_IMMERSIVE
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Hide the nav bar and status bar
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
  }

  @Override
  protected void onStop() {
    database = null;
    QuestionsDatabase.forgetInstance(this);
    super.onStop();
  }


  //initializes data base to fix null pointer exception on OnCreate
  private class InitializeDataBase extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... voids) {
      List<Question> questions = QuestionsDatabase.getInstance(MainActivity.this)
          .getQuestionDao().select();
      if (questions.size() == 0){
        QuestionsDatabase.populateQuestions(MainActivity.this);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        new QueryTask().execute();
    }
  }


  /**
   * The type Query task.
   */
  class QueryTask extends AsyncTask<Void, Void, Question> {

    @Override
    protected void onPostExecute(Question question) {
      super.onPostExecute(question);
      Fragment fragment = null;
      switch (question.getType()) {
        case Question.MULTI_CHOICE:
          fragment = new MultiChoiceFragment();
          break;
        case Question.TRUE_FALSE:
          fragment = new TrueFalseFragment();
          break;
        case Question.MULTI_ANS:
          fragment = new MultiAnswerFragment();
      }
      if (fragment != null) {
        //figure this out how bundles work
        Bundle bundle = new Bundle();
        bundle.putLong(QUESTION_ID, question.getId());
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container_options, fragment)
            .commit();
      }
    }

    @Override
    protected Question doInBackground(Void... voids) {
      database = QuestionsDatabase.getInstance(MainActivity.this);
      return database.getQuestionDao().selectRandomWithAnswers();
    }
  }

  public void reloadQuestion(){
    new QueryTask().execute();
  }

private void saveToSharedPrefs(String s){
  SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
  SharedPreferences.Editor editor = sharedPreferences.edit();
  editor.putString(getString(R.string.string_key),s);
  editor.apply();
}

private String getFromSharedPrefs(){
    SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
    return sharedPreferences.getString("string_key", "");

}




  /**
   * Get last locked time long.
   *
   * @return the long
   */
  public static long getLastLockedTime(){
    return lastLockedTime;
  }

  /**
   * Reset last locked time.
   */
  public static void resetLastLockedTime(){
    lastLockedTime = System.currentTimeMillis();
  }



}

