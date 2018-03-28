package io.panther.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.panther.Panther;
import io.panther.demo.bean.StudentBean;
import io.panther.demo.bean.TradeBean;
import io.panther.demo.bean.TradeBookBean;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View v) {


//            long start = System.currentTimeMillis();
//            Panther.get(this).writeInDatabase("students", studentList);
//            Log.i("panther", "write cost: " + (System.currentTimeMillis() - start) + " ms");
//
//            long start1 = System.currentTimeMillis();
//            List<StudentBean> studentList1 = (List<StudentBean>) Panther.get(this).readObjectFromDatabase("students", StudentBean.class);
//            Log.i("panther", "read cost: " + (System.currentTimeMillis() - start1) + " ms");
//            for (StudentBean s : studentList1) {
//                Log.i("stu", s.toString());
//            }
//
//            Panther.get(this).writeInMemory("students", studentList, true);
//
//            List<StudentBean> studentList2 = (List<StudentBean>) Panther.get(this).readFromMemory("students", true);
//            for (StudentBean s : studentList2) {
//                Log.i("stu2", s.toString());
//            }

        TradeBean trade;

        List<TradeBean> tradeList = new ArrayList<>();

        trade = new TradeBean();
        tradeList.add(trade);

        Panther.get(this).writeInDatabase("trades", tradeList);
        Panther.get(this).readObjectFromDatabase("trades", TradeBean.class);
    }
}