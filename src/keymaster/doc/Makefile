CA_ROOT_TYPES := PCI PII Trivial Mimir
CA_ROOT_TPL_DOT := pci_root.dot
CA_ROOT_LOWER_TYPES :=
CA_ROOT_DOTS :=
CA_ROOT_PNGS :=

define setvars
  lower_type := $$(shell echo $(1) | tr A-Z a-z)
  CA_ROOT_LOWER_TYPES += $$(lower_type)
  root_dot := $$(addsuffix _root.dot,$$(lower_type))
  $$(root_dot)_FRIENDLY := $(1)
  CA_ROOT_DOTS += $$(root_dot)
  CA_ROOT_PNGS += $$(addsuffix _tree.png,$$(lower_type))
endef

$(foreach rt,$(CA_ROOT_TYPES),$(eval $(call setvars,$(rt))))

CA_ROOT_GEN_DOT := $(filter-out $(CA_ROOT_TPL_DOT),$(CA_ROOT_DOTS))

default: $(CA_ROOT_PNGS)

.PHONY: clean
.INTERMEDIATE: $(CA_ROOT_GEN_DOT)

%_tree.png: %_root.dot
	dot -Tpng $^ -o $@

$(CA_ROOT_GEN_DOT): $(CA_ROOT_TPL_DOT)
	sed "s/PCI/$($(@)_FRIENDLY)/g" $^ >$@; done

clean:
	rm -f $(CA_ROOT_PNGS)
