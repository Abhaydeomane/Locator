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
    Button btn_logout,btn_select_img,btn_upload_img;
    TextView textViewEmail,textViewName;
    FirebaseUser user;
    Uri imageUri;
    ImageView profile_img;
    StorageReference storageRef ;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth=FirebaseAuth.getInstance();
        btn_logout=findViewById(R.id.logout);
        btn_select_img=findViewById(R.id.select_img);
        btn_upload_img=findViewById(R.id.upload_img);
        textViewEmail=findViewById(R.id.user_email);
        textViewName=findViewById(R.id.user_name);
        profile_img=findViewById(R.id.profileImageView);
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

        btn_select_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        btn_upload_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });


    }
    private void selectImage(){
        Intent intent = new Intent();
        intent.setType("image/*"); // Specify image MIME type
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==100 && data!=null && data.getData()!=null){
            imageUri=data.getData();
            profile_img.setImageURI(imageUri);
        }
    }
    private void uploadImage(){
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("uploading file...");
        progressDialog.show();

        String filename=user.getEmail()+"-profile_pic";
        storageRef= FirebaseStorage.getInstance().getReference("images/"+filename);
        storageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        profile_img.setImageURI(null);
                        if(progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this,"Successfully uploaded",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,"Failed to upload",Toast.LENGTH_SHORT).show();
                        if(progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                    }
                });

    }
}