package org.nko.chat.ui


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import network.chaintech.cmpimagepickncrop.CMPImagePickNCropDialog
import network.chaintech.cmpimagepickncrop.imagecropper.rememberImageCropper
import org.nko.chat.models.User
import org.nko.chat.util.CompressFormat
import org.nko.chat.util.compressImageBitmap
import org.nko.chat.util.imageBitmapToBase64


@Composable
fun RegisterScreen(
    onNext: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Estados para los campos del formulario, la imagen y la validación
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(Country("Paraguay", "595", "🇵🇾")) }
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var openImagePicker by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    var searchable by remember { mutableStateOf(false) }

    val imageCropper = rememberImageCropper()

    CMPImagePickNCropDialog(
        imageCropper = imageCropper,
        openImagePicker = openImagePicker,
        imagePickerDialogHandler = {
            openImagePicker = it
        },
        selectedImageCallback = {
            selectedImage = compressImageBitmap(it, CompressFormat.JPEG , 80)
        }
    )

    // Función para validar los campos
    fun validateFields(): Boolean {
        var isValid = true
        if (username.text.isEmpty()) {
            usernameError = "Username is required"
            isValid = false
        } else {
            usernameError = null
        }

        // Validación para el campo de teléfono: solo permite números
        if (phoneNumber.isEmpty()) {
            phoneNumberError = "Phone number is required"
            isValid = false
        } else if (!phoneNumber.all { it.isDigit() }) {
            phoneNumberError = "Phone number must contain only digits"
            isValid = false
        }

        return isValid
    }

    // Interfaz de usuario
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Vista previa del avatar seleccionado
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        ) {
            if (selectedImage != null) {
                Image(
                    bitmap = selectedImage!!,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {

                IconButton(
                    onClick = {
                        openImagePicker = true
                    },
                    modifier = Modifier
                        .fillMaxSize().background(Color.Gray, CircleShape)
                ){
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Avatar",
                        tint = Color.White,
                        modifier = Modifier.padding(30.dp).fillMaxSize()
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Campos de formulario
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            isError = usernameError != null
        )
        usernameError?.let {
            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            PhoneNumberPicker(
                modifier = Modifier.fillMaxWidth(),
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it },
                phoneNumber = phoneNumber,
                onPhoneNumberChanged = { phoneNumber = it },
            )


            phoneNumberError?.let {
                Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

        }
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,

        ) {
            Checkbox(
                checked = searchable,
                onCheckedChange = { searchable = it },
                modifier = Modifier.padding(end = 2.dp)
            )
            Text(
                "Permitir que los demás usuarios puedan encontrarme",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { searchable = !searchable }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Botón de siguiente
        Button(
            onClick = {
                if (validateFields()) {
                    onNext(
                        User(
                            id = null,
                            name = username.text,
                            phoneNumber = selectedCountry.code + phoneNumber,
                            email = null,
                            avatar = selectedImage?.let { imageBitmapToBase64(it) },
                            searchable = searchable
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
        }
    }
}





