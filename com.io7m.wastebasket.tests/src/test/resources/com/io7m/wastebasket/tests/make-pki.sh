#!/bin/sh -ex

easyrsa init-pki
easyrsa build-ca
easyrsa gen-req com.io7m.wastebasket.server nopass
easyrsa sign-req client com.io7m.wastebasket.server

