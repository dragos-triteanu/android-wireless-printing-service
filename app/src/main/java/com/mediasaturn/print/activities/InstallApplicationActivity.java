package com.mediasaturn.print.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.mediasaturn.print.init.Installer;

import org.cups.android.R;


public class InstallApplicationActivity extends Activity {

    private TextView installingNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_application);

        installingNotice = (TextView) findViewById(R.id.installingNotice);
        installingNotice.setText(R.string.init);

        if(Installer.isInstalled(this)){
            this.startActivity(new Intent(this,MainActivityRemade.class));
            finish();
        }else{
            installPrerequisites();
        }
    }

    private void installPrerequisites() {
        Installer.installApplicationActivity = this;
        Installer.installingNotice = installingNotice;
        Installer.unpackData();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_install_application, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
