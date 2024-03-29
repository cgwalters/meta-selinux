SUMMARY = "User space tools for kernel auditing"
DESCRIPTION = "The audit package contains the user space utilities for \
storing and searching the audit records generated by the audit subsystem \
in the Linux kernel."
HOMEPAGE = "http://people.redhat.com/sgrubb/audit/"
SECTION = "base"
PR = "r5"
LICENSE = "GPLv2+ & LGPLv2+"
LIC_FILES_CHKSUM = "file://COPYING;md5=94d55d512a9ba36caa9b7df079bae19f"

SRC_URI = "http://people.redhat.com/sgrubb/audit/audit-${PV}.tar.gz \
	   file://disable-ldap.patch \
	   file://audit-python.patch"

SRC_URI += "file://2.2.1-audit-for-cross-compiling.patch \
	   file://audit-python-configure.patch \
	   file://auditd"

inherit autotools pythonnative update-rc.d

UPDATERCPN = "auditd"
INITSCRIPT_NAME = "auditd"
INITSCRIPT_PARAMS = "defaults"

SRC_URI[md5sum] = "dc099fcb2f9242d47ecc35b46d71dfd1"
SRC_URI[sha256sum] = "9865ca89f5b975ccf25441ddf45a874448f2bba944005aa8cd5e3c3148713a63"

DEPENDS += "python tcp-wrappers libcap-ng linux-libc-headers (>= 2.6.30)"

EXTRA_OECONF += "--without-prelude \
	--with-libwrap \
	--enable-gssapi-krb5=no \
	--disable-ldap \
	--with-libcap-ng=yes \
	--with-python=yes \
	--libdir=${base_libdir} \
	--sbindir=${base_sbindir} \
	"

EXTRA_OEMAKE += "PYLIBVER='python${PYTHON_BASEVERSION}' \
	PYINC='${STAGING_INCDIR}/$(PYLIBVER)' \
	pyexecdir=${libdir}/python${PYTHON_BASEVERSION}/site-packages \
	"

SUMMARY_audispd-plugins = "Plugins for the audit event dispatcher"
DESCRIPTION_audispd-plugins = "The audispd-plugins package provides plugins for the real-time \
interface to the audit system, audispd. These plugins can do things \
like relay events to remote machines or analyze events for suspicious \
behavior."

PACKAGES =+ "audispd-plugins"
PACKAGES += "auditd ${PN}-python"

FILES_${PN} = "${sysconfdir}/libaudit.conf ${base_libdir}/libaudit.so.1* ${base_libdir}/libauparse.so.*"
FILES_auditd += "${bindir}/* ${base_sbindir}/* ${sysconfdir}/*"
FILES_audispd-plugins += "${sysconfdir}/audisp/audisp-remote.conf \
	${sysconfdir}/audisp/plugins.d/au-remote.conf \
	${sbindir}/audisp-remote ${localstatedir}/spool/audit \
	"
FILES_${PN}-dbg += "${libdir}/python${PYTHON_BASEVERSION}/*/.debug"
FILES_${PN}-python = "${libdir}/python${PYTHON_BASEVERSION}"
FILES_${PN}-dev += "${base_libdir}/*.so ${base_libdir}/*.la"

# The executables in lib/, which are named as gen_*_h, will run on the hosts to create
# *_tables.h/*tabs.h header files for the targets.
# In some old hosts, build will fail because some .h files in the old linux-libc-headers (<= 2.6.29)
# has incomplete DEFINE lists for the audit system.
do_compile_prepend() {
	mkdir -p ${S}/lib/linux
	cp -f ${STAGING_INCDIR}/linux/audit.h ${S}/lib/linux/
	cp -f ${STAGING_INCDIR}/linux/elf-em.h ${S}/lib/linux/
	cp -f ${STAGING_INCDIR}/linux/net.h ${S}/lib/linux/
	mkdir -p ${S}/lib/sys
	cp -f ${STAGING_INCDIR}/sys/mount.h ${S}/lib/sys/
	cp -f ${STAGING_INCDIR}/sys/personality.h ${S}/lib/sys/
	mkdir -p ${S}/lib/bits
	cp -f ${STAGING_INCDIR}/bits/socket.h ${S}/lib/bits

	# eglibc-2.16 splits enum __socket_type from bits/socket.h to bits/socket_type.h, so we
	# copy bits/socket_type.h only if it exists.
	test -f ${STAGING_INCDIR}/bits/socket_type.h && \
		cp -f ${STAGING_INCDIR}/bits/socket_type.h ${S}/lib/bits
}

do_install_append() {
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.a
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.la

	# reuse auditd config
	[ ! -e ${D}/etc/default ] && mkdir ${D}/etc/default
	mv ${D}/etc/sysconfig/auditd ${D}/etc/default
	rmdir ${D}/etc/sysconfig/

	# replace init.d
	install -D -m 0755 ${S}/../auditd ${D}/etc/init.d/auditd
	rm -rf ${D}/etc/rc.d
}
