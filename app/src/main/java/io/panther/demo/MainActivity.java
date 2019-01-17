package io.panther.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.panther.Panther;
import io.panther.callback.FindKeysCallback;
import io.panther.callback.MassDeleteCallback;
import io.panther.callback.ReadArrayCallback;
import io.panther.callback.ReadCallback;
import io.panther.callback.WriteCallback;
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
            Panther.get(getActivity()).writeInDatabase("stu_james", studentJames);
            Panther.get(getActivity()).writeInDatabase("test_1", 1);
            Panther.get(getActivity()).writeInDatabase("test_2", 2.0f);
            Panther.get(getActivity()).writeInDatabase("test_3", "3");
            Panther.get(getActivity()).writeInDatabase("test_4", false);

            Panther.get(getActivity()).readStringFromDatabase("stu_jack");
            Panther.get(getActivity()).readIntFromDatabase("test_1", 0);
            Panther.get(getActivity()).readDoubleFromDatabase("test_2", 0);
            Panther.get(getActivity()).readStringFromDatabase("test_3", "");
            Panther.get(getActivity()).readBooleanFromDatabase("test_4", false);

            StudentBean studentTemp = Panther.get(getActivity()).readFromDatabase("stu_jack", StudentBean.class);
            Log.i("studentJack", studentTemp.toString());

            Panther.get(getActivity()).readFromDatabaseAsync("stu_james", StudentBean.class, new ReadCallback<StudentBean>() {
                @Override
                public void onResult(boolean success, StudentBean result) {
                    Log.i("studentJames", result.toString());
                }
            });

            Panther.get(getActivity()).writeInDatabaseAsync("students", students, new WriteCallback() {
                @Override
                public void onResult(boolean success) {
                    Panther.get(getActivity()).readArrayFromDatabaseAsync("students", StudentBean.class, new ReadArrayCallback<StudentBean>() {
                        @Override
                        public void onResult(boolean success, StudentBean[] result) {
                            if (success) {
                                List<StudentBean> students = Arrays.asList(result);
                                Log.i("students", students.size() + " student");
                                for (StudentBean stu : students) {
                                    Log.i("sss", stu.toString());
                                }
                            }
                        }
                    });
                }
            });

            Log.i("students exist", Panther.get(getActivity()).keyExist("students") + "");

            Panther.get(getActivity()).deleteFromDatabase("test_1");
            Log.i("test exist", Panther.get(getActivity()).keyExist("test_1") + "");

            Panther.get(getActivity()).findKeysByPrefix("test", new FindKeysCallback() {
                @Override
                public void onResult(String[] keys) {
                    Panther.get(getActivity()).massDeleteFromDatabaseAsync(keys, new MassDeleteCallback() {
                        @Override
                        public void onResult(boolean b) {
                            Log.i("test exist", Panther.get(getActivity()).keyExist("test_1") + "");
                        }
                    });
                }
            });

        } else if (v.getId() == R.id.btnMainMemoryCache) {
            // not weak ref
            Panther.get(this).writeInMemory("studentJack", studentJack, true);
            // weak ref
            Panther.get(this).writeInMemory("studentJames", studentJames);

            StudentBean jack = Panther.get(this).readFromMemory("studentJack", true);
            if (jack != null)
                Log.i("jack", jack.toString());
            StudentBean james = Panther.get(this).readFromMemory("studentJames");
            if (james != null)
                Log.i("james", james.toString());

            Panther.get(this).deleteFromMemory("studentJames");

            Panther.get(this).writeInMemory("Curry", "Chef Curry!!!");
            Panther.get(this).writeInMemory("Curry", null);
            Panther.get(this).writeInMemory("Curry", null);
            Object curry = Panther.get(this).readFromMemory("Curry");
            if (curry != null) {
                Log.i("curry", curry.toString());
            } else {
                Log.i("curry", "null");
            }
        }
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}