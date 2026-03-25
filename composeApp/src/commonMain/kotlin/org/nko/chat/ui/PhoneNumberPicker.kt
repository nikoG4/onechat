package org.nko.chat.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eygraber.compose.country.code.picker.CountryCodePicker

@Composable
fun PhoneNumberPicker(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    phoneNumber: String,
    onPhoneNumberChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickerVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Selector de país y entrada de número
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent,  OutlinedTextFieldDefaults.shape)
                .border(1.dp, Color.Gray,  OutlinedTextFieldDefaults.shape)
                .clickable { isPickerVisible = true }
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bandera y código de país
            Text(
                text = "${selectedCountry.flag}  +${selectedCountry.code}",
                modifier = Modifier.padding(end = 8.dp),
            )

            // Entrada de número
            TextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChanged,
                placeholder = { Text("Mobile Number") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
            )
        }

        // Ventana emergente para seleccionar el país
        if (isPickerVisible) {
            Dialog(onDismissRequest = { isPickerVisible = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {

                    CountryCodePicker(
                        onClick = { country ->
                            run {
                                onCountrySelected(
                                    Country(
                                        country.name,
                                        country.code,
                                        country.emoji
                                    )
                                )
                                isPickerVisible = false
                            }
                        },
                    )

                }
            }
        }
    }
}

// Datos del país (ejemplo)
data class Country(val name: String, val code: String, val flag: String)
