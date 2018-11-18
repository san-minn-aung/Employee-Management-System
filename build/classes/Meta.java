/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package SamMinAung;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author user
 */
public class Meta {
    
}
class Employee_Attendance implements Serializable {
    
  HashMap<Department,ArrayList<String>> map = new HashMap<Department,ArrayList<String>>();
  
}

class Employee_Working implements Serializable {
  HashMap<String,Integer> map = new HashMap<String,Integer>();
}

enum Department{
    HR,
    IT,
    FINANCE,
    SECURITY
}