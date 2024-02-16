package com.example.locator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileUpdate extends AppCompatActivity {


    Button btn_select_img,btn_upload_img,btn_update_info;
    Uri imageUri;
    ImageView profile_img;
    StorageReference storageRef ;
    ProgressDialog progressDialog;
    String user_uid,user_email;
    TextInputEditText editTextFirstname,editTextLastname,editTextMobile;

    String firstname,lastname,mobile;
    FirebaseDatabase db;
    DatabaseReference reference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_update);

        Intent intent = getIntent();
        if (intent != null) {//taking uid and email from mainactivity
            user_uid= intent.getStringExtra("User_uid");
            user_email= intent.getStringExtra("User_email");
        }
        btn_select_img=findViewById(R.id.select_img);
        btn_upload_img=findViewById(R.id.upload_img);
        btn_update_info=findViewById(R.id.update);
        profile_img=findViewById(R.id.profileImageView);
        editTextFirstname=findViewById(R.id.FirstName);
        editTextLastname=findViewById(R.id.LastName);
        editTextMobile=findViewById(R.id.Mobile);

        //Storing old values of user in class users
        Users users = new Users();
        users.setEmail(user_email);

        //data from realtime database
        db = FirebaseDatabase.getInstance();
        reference = db.getReference("Users");
        reference.child(user_uid).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot dataSnapshot = task.getResult();
                    String firstName_old = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName_old = dataSnapshot.child("lastName").getValue(String.class);
                    String mobile_old = dataSnapshot.child("mobile").getValue(String.class);
                    String imageurl_old=dataSnapshot.child("imageurl").getValue(String.class);

                    // Set the users object
                    users.setFirstName(firstName_old);
                    users.setLastName(lastName_old);
                    users.setMobile(mobile_old);
                    users.setImageurl(imageurl_old);

                    // Set the EditText fields
                    editTextFirstname.setText(users.getFirstName());
                    editTextLastname.setText(users.getLastName());
                    editTextMobile.setText(users.getMobile());

                }
            }
        });

        //Now showing old image from firebase storage
        // Create a storage reference from your app
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        // Create a reference to the image file you want to download
        StorageReference imageRef = storageRef.child("images/"+user_email+"-profile_pic");
        // Download the image into a byte array
        final long ONE_MEGABYTE = 1024 * 1024; // 1MB
        imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Successfully downloaded data to bytes array
                // Convert the byte array to a Bitmap and display it in an ImageView
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profile_img.setImageBitmap(bitmap); // Set the bitmap to your ImageView
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.e("FirebaseStorage", "Fail downloading image", exception);
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

        btn_update_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    firstname=editTextFirstname.getText().toString();
                    lastname=editTextLastname.getText().toString();
                    mobile=editTextMobile.getText().toString();

                    users.setFirstName(firstname);
                    users.setLastName(lastname);
                    users.setMobile(mobile);

                    db = FirebaseDatabase.getInstance();
                    reference = db.getReference("Users");
                    reference.child(user_uid).setValue(users)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(ProfileUpdate.this, "Successfully Update info", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ProfileUpdate.this, "Failed to update", Toast.LENGTH_SHORT).show();
                                }
                            });


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

        String filename=user_email+"-profile_pic";
        storageRef= FirebaseStorage.getInstance().getReference("images/"+filename);
        storageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if(progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                        Toast.makeText(ProfileUpdate.this,"Successfully uploaded",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileUpdate.this,"Failed to upload",Toast.LENGTH_SHORT).show();
                        if(progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                    }
                });

    }
}