package models

sealed trait Permission

case object NormalUser extends Permission
case object AdminUser extends Permission
