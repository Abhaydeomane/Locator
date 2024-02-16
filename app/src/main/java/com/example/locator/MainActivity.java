package com.example.locator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button btn_logout,btn_profile;
    TextView textViewEmail,textViewName;
    FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth=FirebaseAuth.getInstance();
        btn_logout=findViewById(R.id.logout);
        btn_profile=findViewById(R.id.Profile);
        textViewEmail=findViewById(R.id.user_email);
        textViewName=findViewById(R.id.user_name);

        user=auth.getCurrentUser();


        if(user==null){
            Intent intent=new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        else{
            textViewEmail.setText(user.getEmail());
            textViewName.setText(user.getDisplayName());
        }

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent=new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });
        btn_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this, ProfileUpdate.class);
                intent.putExtra("User_uid", user.getUid());
                intent.putExtra("User_email", user.getEmail());
                startActivity(intent);
            }
        });

    }

}