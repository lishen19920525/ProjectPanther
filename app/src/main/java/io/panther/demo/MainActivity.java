package io.panther.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.panther.Panther;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<StudentBean> studentList = new ArrayList<>();
        StudentBean student = new StudentBean();
        student.setId(1000001L);
        student.setGender(1);
        student.setName("Stephen Curry");
        student.setBirthday(new Date(420457396000L));
        studentList.add(student);
        student = new StudentBean();
        student.setId(1000002L);
        student.setGender(1);
        student.setName("LeBron James");
        student.setBirthday(new Date(420357396000L));
        studentList.add(student);
        student = new StudentBean();
        student.setId(1000003L);
        student.setGender(0);
        student.setName("Lily Zhang");
        student.setBirthday(new Date(420157396000L));
        studentList.add(student);
        student = new StudentBean();
        student.setId(1000004L);
        student.setGender(1);
        student.setName("Qi Zhou");
        student.setBirthday(new Date(420657396000L));
        studentList.add(student);

        Panther.get(this).writeInDatabase("students", studentList);

        List<StudentBean> studentList1 = (List<StudentBean>) Panther.get(this).readObjectFromDatabase("students", StudentBean.class);
        for (StudentBean s : studentList1) {
            Log.i("stu", s.toString());
        }

        Panther.get(this).writeInMemory("students", studentList, true);

        List<StudentBean> studentList2 = (List<StudentBean>) Panther.get(this).readFromMemory("students", true);
        for (StudentBean s : studentList2) {
            Log.i("stu2", s.toString());
        }
    }
}