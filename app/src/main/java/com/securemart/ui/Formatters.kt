package com.securemart.ui

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun money(cents: Long): String =
    String.format(Locale.US, "Rs %,.2f", BigDecimal.valueOf(cents, 2))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
