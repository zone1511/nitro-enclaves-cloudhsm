#!/bin/bash
sudo sed -i "s/cpu_count:.*/cpu_count: 4/g" /etc/nitro_enclaves/allocator.yaml
sudo sed -i "s/memory_mib:.*/memory_mib: 6144/g" /etc/nitro_enclaves/allocator.yaml
sudo systemctl restart nitro-enclaves-allocator.service
sudo systemctl enable --now nitro-enclaves-allocator.service