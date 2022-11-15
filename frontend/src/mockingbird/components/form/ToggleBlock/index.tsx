import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import { ToggleBlock as PlatformToggleBlock } from '@platform-ui/block';

interface Props {
  name: string;
  label: string;
  control: Control;
  required?: boolean;
}

export default function ToggleBlock(props: Props) {
  const { name, label, control, required = false } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
  });
  const { onChange } = field;
  const handleChange = useCallback(
    (_, { checked }) => onChange(checked),
    [onChange]
  );
  const { ref, ...fieldProps } = field;
  return (
    <PlatformToggleBlock
      {...fieldProps}
      label={label}
      onChange={handleChange}
      defaultChecked={field.value}
    />
  );
}
