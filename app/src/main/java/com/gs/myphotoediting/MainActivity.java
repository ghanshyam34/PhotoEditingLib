package com.gs.myphotoediting;
import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.myphotoeditinglibrarys.ImageEditorview;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity{

    String permission[] = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };


    ImageEditorview image_editorview;
    ImageView imageview;
    ImageButton editimage;
    Bitmap finalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23 && permission != null) {

            int result;
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String p : permission) {
                result = ContextCompat.checkSelfPermission(this, p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            if (!listPermissionsNeeded.isEmpty()) {

                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),111);
            }
        }

        editimage = (ImageButton)findViewById(R.id.editimage);
        imageview = (ImageView) findViewById(R.id.imageview);

        image_editorview = (ImageEditorview) findViewById(R.id.image_editorview);
        image_editorview.addAttachActivity(MainActivity.this);
        finalBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.aa);
        imageview.setImageBitmap(finalBitmap);

        image_editorview.setPhotoBitmap(finalBitmap);
        image_editorview.setOnResultedBitmapListener(new ImageEditorview.OnResultedBitmapListener() {
            @Override
            public void onResultedBitmap(Bitmap bitmap) {
//                viewBitmapDialog(bitmap);
                image_editorview.hideImageEditor();
                imageview.setImageBitmap(bitmap);
                finalBitmap = bitmap;
            }
        });


        editimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_editorview.setPhotoBitmap(finalBitmap);
                image_editorview.showImageEditor();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        image_editorview.onActivityResult(requestCode, resultCode, data);
    }

    private void viewBitmapDialog(Bitmap bmp) {
        try {
            ImageView imageView = new ImageView(MainActivity.this);
            imageView.setImageBitmap(bmp);
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(imageView);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
