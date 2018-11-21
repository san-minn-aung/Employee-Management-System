
import java.awt.Graphics;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.JXDatePicker;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author MIN THET ZAN
 */
public class PayrollForm extends javax.swing.JFrame {
     DefaultTableModel table_model;
     Connection conn;
     Statement stmt;
     ResultSet rs;
     String sql;
    
     //Working object
     Employee_Working emp_working=new Employee_Working();//initialize employee working object
     int CurrentMonthTotalWorkingDays = 0;
     
     public void createDB(){
        try{
            conn=DriverManager.getConnection("jdbc:ucanaccess://ProjectDatabase.accdb","Admin","");
            System.out.println("DB connected");
            
        }catch(Exception e){
            System.out.println("createDB error!");
        }
    }

     
     private void LoadWorkingObject() throws SQLException{
         createDB();
         DateTimeFormatter dtdate = DateTimeFormatter.ofPattern("MM/yyyy");
         LocalDateTime now = LocalDateTime.now();
         String Current_Month = dtdate.format(now);//get now current month(06/2018)
         
         try {
             //get Employee working object
             sql = "select * from Working where Month_Of_Date = '"+Current_Month+"'";
             stmt = conn.createStatement();
             rs = stmt.executeQuery(sql);
             if(rs.next()){
                 byte[] st = rs.getBytes("Working_Count");
                 ByteArrayInputStream baip = new ByteArrayInputStream(st);
                 ObjectInputStream ois = new ObjectInputStream(baip);
                 emp_working = (Employee_Working) ois.readObject();
                 Set set = emp_working.map.entrySet();
                 System.out.println("found object");
             }
             
             //get Current Month Total working days
             String currentMonth = Current_Month.trim().split("/")[0];
             String currentYear = Current_Month.trim().split("/")[1];
             sql = "select count(*) as rowcount from Attendance where MONTH(Date) = '"+currentMonth+"'  AND YEAR(Date) = '"+currentYear+"'" ;
             stmt = conn.createStatement();
             rs = stmt.executeQuery(sql);
             rs.next();
             CurrentMonthTotalWorkingDays = rs.getInt("rowcount");//get current total days
             
         } catch (SQLException | IOException | ClassNotFoundException ex) {
             Logger.getLogger(PayrollForm.class.getName()).log(Level.SEVERE, null, ex);
         }finally{
             try {
                 stmt.close();
                 conn.close();
             } catch (SQLException ex) {
                 Logger.getLogger(PayrollForm.class.getName()).log(Level.SEVERE, null, ex);
             }
             
         }
         
         
     }
     
      private void Resource_Loader()throws SQLException{
         table_model = (DefaultTableModel)jTable1.getModel();
         table_model.setRowCount(0);
         
         DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
         centerRenderer.setHorizontalAlignment( JLabel.CENTER );
         jTable1.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
         jTable1.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );
         jTable1.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );
         createDB();
         try {
             stmt = conn.createStatement();
             sql="select * from Employee";
             rs=stmt.executeQuery(sql);
             while(rs.next()){
                 int workingCount = getWorkingCount(rs.getString("ID"));//get working count
                 int absenceCount = CurrentMonthTotalWorkingDays - workingCount;
                 float netSalary =  (rs.getInt("Basic_Salary")/ CurrentMonthTotalWorkingDays)*workingCount;
                 populate(rs.getString("ID"), rs.getString("Name"),rs.getString("Department"),rs.getInt("Basic_Salary"), workingCount, absenceCount, netSalary);//,rs.getInt("PresentDay"),rs.getInt("AbsentDay"),rs.getInt("NetSalary"));//true is office present
             }
         } catch (SQLException ex) {
             Logger.getLogger(PayrollForm.class.getName()).log(Level.SEVERE, null, ex);
         }finally{
             try {
                 stmt.close();
                 conn.close();
             } catch (SQLException ex) {
                 Logger.getLogger(PayrollForm.class.getName()).log(Level.SEVERE, null, ex);
             }
         }
      }  
      
      private int getWorkingCount(String _id){
          //track data
          Set set = emp_working.map.entrySet();
          Iterator iterator = set.iterator();
          int count = 0;
          System.out.println(_id);
          while(iterator.hasNext()) {
              System.out.println("has next");
              Map.Entry entry = (Map.Entry)iterator.next();
              System.out.println(entry.getKey());
              if(entry.getKey().equals(_id))
              {
                  System.out.println("found");
                  count = (int)entry.getValue();
                  break;
              }
          }
          return count;
      }
 
      public void populate(String _ID, String _Name, String _Department, float _Salary, int _PresentDay, int _absenceDay, float _netSalary){//int _PresentDay, int _AbsentDay, int _NetSalary
        Object[] rowData = {_ID, _Name,_Department,_Salary, _PresentDay, _absenceDay, _netSalary};
        table_model.addRow(rowData);            
    }
    /**
     * Creates new form PayrollForm
     */
    public PayrollForm() throws SQLException {
        initComponents();
        LoadWorkingObject();
        Resource_Loader();
        Time_Loader();
         
    }
   
private void Time_Loader(){
        JXDatePicker picker=new JXDatePicker();
        picker.setFormats(new String[]{"dd MMMM yyyy"});
        DateTimeFormatter dttime = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter dtdate = DateTimeFormatter.ofPattern("dd MMMM yyy");
        picker.setDate(new Date());
        final Timer updater = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LocalDateTime now = LocalDateTime.now();                
                lblDate.setText(dttime.format(now));                
            }
        });
        updater.start();
    }
 private void filter(String query1){
        TableRowSorter<DefaultTableModel> tr = new TableRowSorter<DefaultTableModel>(table_model);
        jTable1.setRowSorter(tr);
        tr.setRowFilter(RowFilter.regexFilter(query1));
        
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jPanel3 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        lblDate = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("EMPLOYEE MANAGEMENT SYSTEM                                       PAYROLL FORM");
        setLocation(new java.awt.Point(300, 100));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        jPanel2.setBackground(new java.awt.Color(0, 0, 0));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Department", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N

        jComboBox1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "DEPARTMENTS", "HR", "IT", "FINANCE", "SECURITY" }));
        jComboBox1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jComboBox1, 0, 141, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jComboBox1, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
        );

        jPanel3.setBackground(new java.awt.Color(0, 0, 0));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Search", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N

        jTextField1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(255, 0, 0), null));
        jTextField1.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                jTextField1CaretUpdate(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTextField1)
        );

        lblDate.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblDate.setForeground(new java.awt.Color(255, 255, 255));
        lblDate.setText("Running Date");

        jPanel4.setBackground(new java.awt.Color(0, 0, 0));

        jTable1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Name", "Department", "Basic Salary", "Present Day", "Absent Day", "Net  Salary"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Float.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 706, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 686, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 291, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 220, Short.MAX_VALUE)
                .addComponent(lblDate, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(lblDate, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(344, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap(82, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(39, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1CaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_jTextField1CaretUpdate
        // TODO add your handling code here:
        String query1=jTextField1.getText();
        filter(query1);
    }//GEN-LAST:event_jTextField1CaretUpdate

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        String query1=jComboBox1.getSelectedItem().toString();
        filter(query1);
    }//GEN-LAST:event_jComboBox1ActionPerformed
     
    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        int index=jTable1.getSelectedRow();
        table_model = (DefaultTableModel)jTable1.getModel();
        String id=table_model.getValueAt(index,0).toString();
        String netSalary=table_model.getValueAt(index,6).toString();
        SalarySlip salary = new SalarySlip(id,netSalary);        
        salary.setVisible(true);
        salary.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salary.pack();
        salary.setLocationRelativeTo(null);
        this.dispose();

        
    }//GEN-LAST:event_jTable1MouseClicked

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        new HomePageForm().setVisible(true);
    }//GEN-LAST:event_formWindowClosed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PayrollForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PayrollForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PayrollForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PayrollForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               
                try {
                    new PayrollForm().setVisible(true);
                } catch (SQLException ex) {
                    Logger.getLogger(PayrollForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel lblDate;
    // End of variables declaration//GEN-END:variables
}
