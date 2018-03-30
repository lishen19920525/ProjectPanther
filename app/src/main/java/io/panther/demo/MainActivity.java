package io.panther.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.panther.Panther;
import io.panther.callback.MassDeleteCallback;
import io.panther.callback.ReadCallback;
import io.panther.callback.SaveCallback;
import io.panther.demo.bean.Gender;
import io.panther.demo.bean.StudentBean;

public class MainActivity extends Activity {
    private List<StudentBean> students = new ArrayList<>();
    private StudentBean studentJack;
    private StudentBean studentJames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Panther.get(this).openDatabase();

        studentJack = new StudentBean();
        studentJack.setCardBalance(new BigDecimal(9.1212111102233));
        studentJack.setBirthday(new Date(701857136000L));
        studentJack.setCredit(-9.2F);
        studentJack.setValid(true);
        studentJack.setId(100000001L);
        studentJack.setName("Jack Mao 😯");
        studentJack.setGender(Gender.MALE);
        studentJack.setGrade(12);
        students.add(studentJack);


        studentJames = new StudentBean();
        studentJames.setCardBalance(new BigDecimal(0.0000));
        studentJames.setBirthday(new Date(600857136000L));
        studentJames.setValid(true);
        studentJames.setId(100000002L);
        studentJames.setName("LeBron James 😄");
        studentJames.setGender(Gender.MALE);
        students.add(studentJames);

        StudentBean student = new StudentBean();

        student = new StudentBean();
        student.setCardBalance(new BigDecimal("-3.33"));
        student.setBirthday(new Date(401857136000L));
        student.setCredit(100F);
        student.setValid(false);
        student.setId(100000003L);
        student.setName("\t\n\r\\\\");
        student.setGender(Gender.FEMALE);
        student.setGrade(0);
        students.add(student);

        student = new StudentBean();
        student.setCardBalance(new BigDecimal(199244.21));
        student.setBirthday(new Date(801857136000L));
        student.setCredit(0.5F);
        student.setValid(true);
        student.setId(100000004L);
        student.setName("Dwayne Wade");
        student.setGrade(10);
        students.add(student);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btnMainDatabase) {
            Panther.get(getActivity()).writeInDatabase("stu_jack", studentJack);
            Panther.get(getActivity()).writeInDatabase("test_1", 1);
            Panther.get(getActivity()).writeInDatabase("test_2", 2.0f);
            Panther.get(getActivity()).writeInDatabase("test_3", "3");
            Panther.get(getActivity()).writeInDatabase("test_4", false);

            Panther.get(getActivity()).readStringFromDatabase("stu_jack");
            Panther.get(getActivity()).readIntFromDatabase("test_1", 0);
            Panther.get(getActivity()).readDoubleFromDatabase("test_2", 0);
            Panther.get(getActivity()).readStringFromDatabase("test_3", "");
            Panther.get(getActivity()).readBooleanFromDatabase("test_4", false);

            StudentBean studentTemp = (StudentBean) Panther.get(getActivity()).readFromDatabase("stu_jack", StudentBean.class);
            Log.i("studentJack", studentTemp.toString());

            Panther.get(getActivity()).writeInDatabaseAsync("students", students, new SaveCallback() {
                @Override
                public void onResult(boolean success) {
                    Panther.get(getActivity()).readFromDatabaseAsync("students", StudentBean.class, new ReadCallback() {
                        @Override
                        public void onResult(boolean success, Object result) {
                            if (success) {
                                List<StudentBean> students = (List<StudentBean>) result;
                                Log.i("students", students.size() + " student");
                            }
                        }
                    });
                }
            });

            Log.i("students exist", Panther.get(getActivity()).keyExist("students") + "");

            Panther.get(getActivity()).deleteFromDatabase("test_1");
            Log.i("test exist", Panther.get(getActivity()).keyExist("test_1") + "");

            String[] keys = Panther.get(getActivity()).findKeysByPrefix("test");
            Panther.get(getActivity()).massDeleteFromDatabaseAsync(keys, new MassDeleteCallback() {
                @Override
                public void onResult(boolean b) {
                    Log.i("test exist", Panther.get(getActivity()).keyExist("test_1") + "");
                }
            });
        } else if (v.getId() == R.id.btnMainMemoryCache) {

        }
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Panther.get(getActivity()).closeDatabase();
    }
}